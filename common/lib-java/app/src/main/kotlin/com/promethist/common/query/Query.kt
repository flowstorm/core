package com.promethist.common.query

data class Query(
        val limit: Int = 20,
        val seek_id: String?,
        val filters: MutableList<Filter> = mutableListOf()
) {
    data class Filter(val name: String, val operator: Operator, val value: String)

    enum class Operator {
        eq, gt, gte,  lt, lte, `in`, like, regex
    }
}