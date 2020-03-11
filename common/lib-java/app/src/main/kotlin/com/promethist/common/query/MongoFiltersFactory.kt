package com.promethist.common.query

import org.bson.conversions.Bson
import org.litote.kmongo.*
import org.litote.kmongo.id.ObjectIdGenerator
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import javax.ws.rs.WebApplicationException
import javax.ws.rs.core.Response
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.memberProperties

class MongoFiltersFactory {
    companion object {
        fun createFilters(
                model: KClass<*>,
                query: Query,
                seekProperty: KProperty<*>? = model.memberProperties.firstOrNull { it.name == "_id" },
                includeSeek: Boolean = true //todo refactor to have two methods for filters and seek filters
        ): MutableList<Bson> {

            val filters = mutableListOf<Bson>()

            if (query.seek_id != null && includeSeek) {
                filters.add(seekProperty!! gt ObjectIdGenerator.create(query.seek_id!!))
            }

            for (filter in query.filters) {
                val property = model.memberProperties.firstOrNull { it.name == filter.name } ?: continue
                val value = if (filter.operator == Query.Operator.`in`) {
                    filter.value.split(",").map { createValue(property, it) }
                } else {
                    createValue(property, filter.value)
                }

                when (filter.operator) {
                    Query.Operator.eq -> filters.add(property eq value)
                    Query.Operator.gt -> filters.add(property gt value)
                    Query.Operator.gte -> filters.add(property gte value)
                    Query.Operator.lt -> filters.add(property lt value)
                    Query.Operator.lte -> filters.add(property lte value)
                    Query.Operator.`in` -> filters.add(property `in` value as List<*>)
                    Query.Operator.like -> filters.add(property as KProperty<String> regex ".*${value}.*")
                    Query.Operator.regex -> filters.add(property as KProperty<String> regex value as String)
                }
            }

            return filters
        }

        private fun createValue(property: KProperty<*>, value: String): Any {
            if (property.returnType.isSubtypeOf(Id::class.createType(listOf(KTypeProjection.STAR)))) {
                return ObjectIdGenerator.create(value)
            }
            return when (property.returnType) {
                Int::class.createType() -> value.toInt()
                Date::class.createType()-> getDateFromString(value)
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
}