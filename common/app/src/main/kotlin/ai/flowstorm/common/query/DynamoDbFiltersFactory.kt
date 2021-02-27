package ai.flowstorm.common.query

import java.text.SimpleDateFormat
import java.util.*
import com.amazonaws.services.dynamodbv2.document.utils.NameMap
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap
import javax.ws.rs.NotSupportedException

object DynamoDbFiltersFactory {

    fun createFilters(
        query: Query,
        filterExpression: MutableList<String> = mutableListOf(),
        keywordExpression: MutableList<String> = mutableListOf(),
        nameMap: NameMap = NameMap(),
        valueMap: ValueMap = ValueMap(),
        indexValues: List<String> = listOf(),
        datetime: Date? = null
    ): Quadruple {
        val datetimes = query.filters.filter { filter -> filter.path == "datetime" }.sortedBy { item -> item.value }.toList()

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
            if (field in indexValues){
                if (field != "datetime" || datetimes.size < 2) {
                    keywordExpression.add("#row$index $operator :placeholder$index")
                    nameMap.with("#row$index", field)
                    valueMap.withString(":placeholder$index", filter.value)
                }
            } else {
                filterExpression.add("#row$index $operator :placeholder$index")
                nameMap.with("#row$index", field)
                valueMap.withString(":placeholder$index", filter.value)
            }
        }
        if (datetimes.size == 2 && "datetime" in indexValues) {
            keywordExpression.add("#placeholderTime BETWEEN :lowerbound AND :upperbound")
            nameMap.with("#placeholderTime", "datetime")
            valueMap.withString(":lowerbound", datetimes[0].value)
            valueMap.withString(":upperbound", datetime?.let { SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(it) } ?: datetimes[1].value)
        }
        if (datetimes.isEmpty() && datetime != null && "datetime" in indexValues) {
            keywordExpression.add("#placeholderTime  <= :upperbound")
            nameMap.with("#placeholderTime", "datetime")
            valueMap.withString(":upperbound", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(datetime))
        }
        return Quadruple(filterExpression, keywordExpression, nameMap, valueMap)
    }
    data class Quadruple(val A: MutableList<String>, val B: MutableList<String>, val C: NameMap, val D: ValueMap )
}
