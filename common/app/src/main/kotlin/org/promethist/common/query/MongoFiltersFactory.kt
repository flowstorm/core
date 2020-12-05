package org.promethist.common.query

import com.mongodb.client.model.Filters
import org.bson.conversions.Bson
import org.litote.kmongo.*
import org.litote.kmongo.id.ObjectIdGenerator
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import javax.ws.rs.WebApplicationException
import javax.ws.rs.core.Response
import kotlin.reflect.*
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.memberProperties

object MongoFiltersFactory {
    fun createFilters(
            rootModel: KClass<*>,
            query: Query,
            seekProperty: KProperty<*>? = rootModel.memberProperties.firstOrNull { it.name == "_id" },
            includeSeek: Boolean = true //todo refactor to have two methods for filters and seek filters
    ): MutableList<Bson> {

        val filters = mutableListOf<Bson>()

        if (query.seek_id != null && includeSeek) {
            filters.add(seekProperty!! gt ObjectIdGenerator.create(query.seek_id))
        }

        for (filter in query.filters) {
            filters.add(createFilter(rootModel, filter))
        }

        return filters
    }

    fun createFilter(rootModel: KClass<*>, filter: Query.Filter): Bson {
        val type = resolveType(rootModel, filter.path)
        val value = if (filter.operator == Query.Operator.`in`) {
            filter.value.split(",").map { createValue(type, it) }
        } else {
            createValue(type, filter.value)
        }

        return when (filter.operator) {
            Query.Operator.eq -> Filters.eq(filter.path, value)
            Query.Operator.gt -> Filters.gt(filter.path, value)
            Query.Operator.gte -> Filters.gte(filter.path, value)
            Query.Operator.lt -> Filters.lt(filter.path, value)
            Query.Operator.lte -> Filters.lte(filter.path, value)
            Query.Operator.`in` -> Filters.`in`(filter.path, value as List<*>)
            Query.Operator.like -> Filters.regex(filter.path, ".*${value}.*")
            Query.Operator.regex -> Filters.regex(filter.path, value as String)
        }
    }

    fun resolveType(rootModel: KClass<*>, path: String): KType {
        val chunks = path.split(".").toMutableList()
        var model = rootModel

        var type: KType

        do {
            val chunk = chunks.removeAt(0)
            var property: KProperty<*> = model.memberProperties.first { it.name == chunk }
            type = property.returnType
            if (type.isSubtypeOf(Iterable::class.createType(listOf(KTypeProjection.STAR)))) {
                type = type.arguments[0].type!!
            }
            if (type.isSubtypeOf(Map::class.createType(listOf(KTypeProjection(KVariance.INVARIANT, String::class.createType()), KTypeProjection.STAR)))) {
                type = type.arguments[1].type!!
                break
            }
            model = type.classifier as KClass<*>
        } while (chunks.isNotEmpty())

        return type
    }

    fun createPipeline(
            model: KClass<*>,
            query: Query,
            seekProperty: KProperty<*>? = model.memberProperties.firstOrNull { it.name == "_id" }
    ): MutableList<Bson> = mutableListOf<Bson>().apply {
        add(match(*createFilters(model, query).toTypedArray()))
        add(sort(orderBy(seekProperty as KProperty<*>)))
        add(limit(query.limit))
    }

    private fun createValue(type: KType, value: String): Any {
        if (type.isSubtypeOf(Id::class.createType(listOf(KTypeProjection.STAR), true))) {
            return ObjectIdGenerator.create(value)
        }
        return when (type) {
            Int::class.createType() -> value.toInt()
            Date::class.createType() -> getDateFromString(value)
            Boolean::class.createType() -> value.toBoolean()
            else -> value
        }
    }

    private fun getDateFromString(dateString: String): Date {
        try {
            val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX")
            return df.parse(dateString)
        } catch (e: ParseException) {
            throw WebApplicationException("Date format should be yyyy-MM-dd'T'HH:mm:ssX", Response.Status.BAD_REQUEST);
        }
    }
}
