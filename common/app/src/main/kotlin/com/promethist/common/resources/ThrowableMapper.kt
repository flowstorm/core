package com.promethist.common.resources

import com.promethist.common.AppConfig
import javax.ws.rs.ServerErrorException
import javax.ws.rs.WebApplicationException
import javax.ws.rs.core.Response
import javax.ws.rs.ext.ExceptionMapper
import javax.ws.rs.ext.Provider

@Provider
class ThrowableMapper : ExceptionMapper<Throwable> {
    override fun toResponse(t: Throwable): Response {
        t.printStackTrace()
        val e = if (t is WebApplicationException) t
        else ServerErrorException(t.message, Response.Status.INTERNAL_SERVER_ERROR, t)

        return Response.fromResponse(e.response)
                .entity("${AppConfig.instance["name"]}:${e::class.java.simpleName}: ${e.message?:""}")
                .type("text/plain")
                .build()
    }
}