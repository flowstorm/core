package com.promethist.common.query

import com.promethist.common.query.Query.Filter
import com.promethist.common.query.Query.Operator
import org.glassfish.hk2.api.Factory
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.core.Context

class QueryValueFactory : Factory<Query> {
    @Context
    lateinit var context: ContainerRequestContext

    companion object {
        const val LIMIT = "limit"
        const val SEEK_ID = "seek_id"

        var reservedParams = listOf(LIMIT, SEEK_ID)
    }

    override fun provide(): Query {
        val r ="(?<field>[A-Za-z0-9_.]*)\\[(?<operator>\\w*)]".toRegex()
        val filters = mutableListOf<Filter>()

        for (param in context.uriInfo.queryParameters.filter { !reservedParams.contains(it.key) }) {
                if (r.matches(param.key)) {
                    val field = r.matchEntire(param.key)!!.groups[1]!!.value
                    val operator = r.matchEntire(param.key)!!.groups[2]!!.value
                    filters.add(Filter(field, Operator.valueOf(operator), param.value.first()))
                }
            }

        return Query(
                limit = context.uriInfo.queryParameters.getFirst(LIMIT)?.toInt() ?: Query.LIMIT_DEFAULT,
                seek_id = context.uriInfo.queryParameters.getFirst(SEEK_ID),
                filters = filters
        )
    }

    override fun dispose(instance: Query) {}
}