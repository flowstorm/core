package com.promethistai.common.resources

import javax.ws.rs.WebApplicationException
import javax.ws.rs.core.Response
import javax.ws.rs.ext.ExceptionMapper
import javax.ws.rs.ext.Provider

@Provider
class ThrowableMapper : ExceptionMapper<Throwable> {
    override fun toResponse(t: Throwable): Response {
        t.printStackTrace()
        if (t is WebApplicationException)
            return t.response
        else
            return Response.status(503).entity(t.message + "\n").type("text/plain").build()
    }
}