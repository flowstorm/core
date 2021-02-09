package org.promethist.core.resources

import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.BsonField
import com.mongodb.client.model.Field
import com.mongodb.client.model.Filters
import org.bson.Document
import org.bson.conversions.Bson
import org.litote.kmongo.*
import org.promethist.common.query.MongoFiltersFactory
import org.promethist.common.query.Query
import org.promethist.common.security.Authorized
import org.promethist.core.model.Application
import org.promethist.core.model.Report
import org.promethist.core.model.Session
import org.promethist.core.model.User
import org.promethist.core.model.metrics.Metric
import org.promethist.core.type.PropertyMap
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.WebApplicationException
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import kotlin.collections.toList

@Path("/reports")
@Produces(MediaType.APPLICATION_JSON)
@Authorized
class ReportResourceImpl: ReportResource {

    @Inject
    lateinit var database: MongoDatabase

    @Inject
    lateinit var query: Query

    private val sessions by lazy { database.getCollection<Session>() }

    override fun getMetrics(): List<PropertyMap> {
        return sessions.aggregate<PropertyMap>(
                match(*propertiesFilters().toTypedArray()),
                unwind("\$metrics"),
                group(Session::metrics / Metric::name),
                project(MetricItem::metric from "\$_id")
        ).toList()
    }

    override fun getNamespaces(): List<PropertyMap> {
        return sessions.aggregate<PropertyMap>(
                match(*propertiesFilters().toTypedArray()),
                unwind("\$metrics"),
                group(Session::metrics / Metric::namespace),
                project(MetricItem::namespace from "\$_id")
        ).toList()
    }

    private fun getDateFromString(dateString: String): Date {
        try {
            val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX")
            return df.parse(dateString)
        } catch (e: ParseException) {
            throw WebApplicationException("Date format should be yyyy-MM-dd'T'HH:mm:ssX", Response.Status.BAD_REQUEST);
        }
    }

    private fun getDatesBetween(startDate: Date, endDate: Date, granularity: Report.Granularity?): MutableList<Date> {
        val timedelta = when (granularity ?: Report.Granularity.DAY) {
//            Report.Granularity.MINUTE -> Calendar.MINUTE
            Report.Granularity.HOUR -> Calendar.HOUR
            Report.Granularity.DAY -> Calendar.DATE
//            Report.Granularity.WEEK -> Calendar.WEEK_OF_YEAR
            Report.Granularity.MONTH -> Calendar.MONTH
            else -> Calendar.DATE
        }

        val datesInRange: MutableList<Date> = mutableListOf()
        val calendar: Calendar = GregorianCalendar()
        calendar.time = startDate
        val endCalendar: Calendar = GregorianCalendar()
        endCalendar.time = endDate
        while (calendar.before(endCalendar)) {
            val result = calendar.time
            datesInRange.add(result)
            calendar.add(timedelta, 1)
        }
        return datesInRange
    }

    override fun getData(
            granularity: Report.Granularity,
            aggregations: List<String>
    ): Report {

        //Jersey produce warning when aggregations parameter is List<Report.Aggregation>
        @Suppress("NAME_SHADOWING")
        val aggregations = aggregations.map { Report.Aggregation.valueOf(it) }

        val start = getDateFromString(query.filters.firstOrNull() { it.path == Session::datetime.name && it.operator == Query.Operator.gte }!!.value)
        val end = getDateFromString(query.filters.firstOrNull() { it.path == Session::datetime.name && it.operator == Query.Operator.lte }!!.value)
        val dates = getDatesBetween(start, end, granularity)

        val pipeline: MutableList<Bson> = mutableListOf()

        // Apply filters
        pipeline.add(match(*MongoFiltersFactory.createFilters(Session::class, query).toTypedArray()))

        pipeline.add(unwind("\$metrics"))

        //add some filters again after unwind
        pipeline.add(match(*
        query.filters.filter { it.path in listOf("metrics.name", "metrics.namespace") }
                .map { MongoFiltersFactory.createFilter(Session::class, it) }
                .toTypedArray()
        ))
        // Add datetime in format based on granularity
        val dateFieldExpression = Document("\$dateToString", Document("date", "\$datetime").append("format", getMongoFormat(granularity)).append("timezone", "GMT"))
        pipeline.add(addFields(Field(MetricItem::date.name, dateFieldExpression)))

        // Apply aggregations
        val aggregationFields = createAggregationFields(aggregations)
        val expression = Document.parse("{\$first: {\$concat: [\"\$user.name\", \" \", \"\$user.surname\"]}}")
        val expression2 = Document.parse("{\$first: \"\$application.name\"}")

        pipeline.add(group(fields(*aggregationFields.toTypedArray()), MetricItem::value sum Session::metrics / Metric::value,
                BsonField(MetricItem::username.name, expression),
                BsonField(MetricItem::applicationName.name, expression2)

        ))

        // Project final columns
        pipeline.add(project(
                MetricItem::user_id from "\$_id.user_id",
                MetricItem::application_id from "\$_id.application_id",
                MetricItem::space_id from "\$_id.space_id",
                MetricItem::namespace from "\$_id.namespace",
                MetricItem::metric from "\$_id.metric",
                MetricItem::date from "\$_id.date",
                MetricItem::value from MetricItem::value,
                MetricItem::username from MetricItem::username,
                MetricItem::applicationName from MetricItem::applicationName
        ))

        // Finally load data
        val data = sessions.aggregate<MetricItem>(*pipeline.toTypedArray()).toList()
        val dataSets: MutableMap<String, Report.DataSet> = mutableMapOf()
        val columns = dates.map { SimpleDateFormat(getDateFormat(granularity)).format(it) }

        for (item in data) {
            val key = getDatasetKey(item)
            val dataSet = if (dataSets.containsKey(key)) dataSets[key]!! else {
                dataSets[key] = Report.DataSet(getDatasetLabel(item, aggregations), MutableList(dates.size) { 0L })
                dataSets[key]!!
            }

            val index = columns.indexOf(item.date)
            dataSet.data[index] = item.value
        }

        val report = Report(columns)
        dataSets.forEach { dataSet -> report.dataSets.add(dataSet.value) }

        return report
    }

    private fun propertiesFilters(): List<Bson> =
            query.filters.filter { it.path.startsWith(Session::properties.name) }.map { Filters.eq(it.path, it.value) }

    private fun getDatasetKey(item: MetricItem): String =
            listOf(item.user_id.toString(), item.namespace, item.metric, item.application_id.toString(), item.space_id.toString()).joinToString(separator = ":")


    private fun getDatasetLabel(item: MetricItem, aggregations: List<Report.Aggregation>): String {
        val keyList = mutableListOf<String>()

        for (aggregation in aggregations) {
            keyList.add(when (aggregation) {
                Report.Aggregation.USER -> item.username!!
                Report.Aggregation.NAMESPACE -> item.namespace!!
                Report.Aggregation.METRIC -> item.metric!!
                Report.Aggregation.APPLICATION -> item.applicationName!!
                Report.Aggregation.SPACE -> "SPACE[" + item.space_id!! + "]"
            })
        }

        return keyList.joinToString(separator = " : ")
    }

    private fun createAggregationFields(aggregation: List<Report.Aggregation>): MutableList<Bson> {
        val aggregationFields = mutableListOf<Bson>(MetricItem::date from MetricItem::date)

        if (aggregation.contains(Report.Aggregation.USER)) {
            aggregationFields.add(MetricItem::user_id from (Session::user / User::_id))
        }

        if (aggregation.contains(Report.Aggregation.NAMESPACE)) {
            aggregationFields.add(MetricItem::namespace from (Session::metrics / Metric::namespace))
        }

        if (aggregation.contains(Report.Aggregation.METRIC)) {
            aggregationFields.add(MetricItem::metric from (Session::metrics / Metric::name))
        }

        if (aggregation.contains(Report.Aggregation.APPLICATION)) {
            aggregationFields.add(MetricItem::application_id from (Session::application / Application::_id))
        }

        if (aggregation.contains(Report.Aggregation.SPACE)) {
            aggregationFields.add(MetricItem::space_id from Session::space_id)
        }

        return aggregationFields
    }

    private fun getMongoFormat(granularity: Report.Granularity): String =
            when (granularity) {
//                Report.Granularity.MINUTE -> "%Y-%m-%d %H:%M"
                Report.Granularity.HOUR -> "%Y-%m-%d %H:00"
                Report.Granularity.DAY -> "%Y-%m-%d"
//                Report.Granularity.WEEK -> "%Y-%V"
                Report.Granularity.MONTH -> "%Y-%m"
            }

    private fun getDateFormat(granularity: Report.Granularity): String =
            when (granularity) {
//                Report.Granularity.MINUTE -> "yyyy-MM-dd HH:mm"
                Report.Granularity.HOUR -> "yyyy-MM-dd HH:00"
                Report.Granularity.DAY -> "yyyy-MM-dd"
//                Report.Granularity.WEEK -> "yyyy-w"  //todo check
                Report.Granularity.MONTH -> "yyyy-MM"
            }

    data class MetricItem(
            val date: String,
            val user_id: Id<User>?,
            val username: String?,
            val application_id: Id<Application>?,
            val applicationName: String?,
            val space_id: String?,
            val namespace: String?,
            val metric: String?,
            val value: Long
    )
}