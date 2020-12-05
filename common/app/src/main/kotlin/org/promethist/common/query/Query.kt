package org.promethist.common.query

import javax.ws.rs.client.WebTarget

data class Query(
        val limit: Int = LIMIT_DEFAULT,
        val seek_id: String?,
        val filters: List<Filter> = listOf<Filter>()
) {
    data class Filter(val path: String, val operator: Operator, val value: String)

    enum class Operator {
        eq, gt, gte, lt, lte, `in`, like, regex
    }
    companion object {
        const val LIMIT_DEFAULT = 100
    }
}

fun WebTarget.query(query: Query): WebTarget {
    var wt = this.queryParam(query::limit.name, query.limit)
    wt = wt.queryParam(query::seek_id.name, query.seek_id)

    for (f in query.filters) {
        wt = wt.queryParam("${f.path}[${f.operator.name}]", f.value)
    }

    return wt
}