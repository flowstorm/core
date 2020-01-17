package com.promethistai.common.resources

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
                .entity("HTTP ${e.response.statusInfo.statusCode} ${e.response.statusInfo.reasonPhrase}\n${e.message}\n")
                .type("text/plain")
                .build()
    }
}