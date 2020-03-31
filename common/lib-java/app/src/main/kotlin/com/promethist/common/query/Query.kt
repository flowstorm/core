package com.promethist.common.query

import javax.ws.rs.client.WebTarget

data class Query(
        val limit: Int = 20,
        val seek_id: String?,
        val filters: MutableList<Filter> = mutableListOf()
) {
    data class Filter(val name: String, val operator: Operator, val value: String)

    enum class Operator {
        eq, gt, gte, lt, lte, `in`, like, regex
    }
}

fun WebTarget.query(query: Query): WebTarget {
    var wt = this.queryParam(query::limit.name, query.limit)
    wt = wt.queryParam(query::seek_id.name, query.seek_id)

    for (f in query.filters) {
        wt = wt.queryParam("${f.name}[${f.operator.name}]", f.value)
    }

    return wt
}