package org.promethist.common.query

import com.amazonaws.services.dynamodbv2.document.KeyAttribute
import java.text.SimpleDateFormat
import java.util.*
import kotlin.reflect.*
import com.amazonaws.services.dynamodbv2.document.utils.NameMap
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap
import org.promethist.common.ObjectUtil
import javax.ws.rs.NotSupportedException

object DynamoDbFiltersFactory {

    fun handleDatetime(
        query: Query,
        datetime: Date,
        filterExpression: MutableList<String>,
        nameMap: NameMap,
        valueMap: ValueMap
    ) {
        filterExpression.add("( ( #datetime = :time and #id <> :id ) or #datetime < :time )")
        nameMap.with("#id", "_id").with("#datetime", "datetime")
        valueMap.withString(":time", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(datetime)).withString(":id", query.seek_id)
    }

    fun updateFilters(
        query: Query,
        filterExpression: MutableList<String>,
        nameMap: NameMap,
        valueMap: ValueMap
    ) = createFilters(query, filterExpression, nameMap, valueMap).let { }

    fun createFilters(
        query: Query,
        filterExpression: MutableList<String> = mutableListOf(),
        nameMap: NameMap = NameMap(),
        valueMap: ValueMap = ValueMap()
    ): Triple<MutableList<String>, NameMap, ValueMap> {
        query.filters.forEachIndexed { index, filter ->
            val operator = when (filter.operator) {
                Query.Operator.eq -> " = "
                Query.Operator.gt -> " > "
                Query.Operator.gte -> " >= "
                Query.Operator.lt -> " < "
                Query.Operator.lte -> " <= "
                Query.Operator.`in` -> throw NotSupportedException("in not supported for DynamoDb")
                Query.Operator.like -> throw NotSupportedException("like not supported for DynamoDb")
                Query.Operator.regex -> throw NotSupportedException("regex not supported for DynamoDb")
            }
            val field = filter.path
            filterExpression.add("#row$index $operator :placeholder$index")
            nameMap.with("#row$index", field)
            valueMap.withString(":placeholder$index", filter.value)
        }
        return Triple(filterExpression, nameMap, valueMap)
    }

}
