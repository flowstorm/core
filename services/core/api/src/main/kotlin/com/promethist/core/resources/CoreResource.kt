package com.promethist.core.resources

import com.promethist.core.Request
import com.promethist.core.Response
import io.swagger.annotations.Api
import io.swagger.annotations.ApiParam
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@Api(description = "Core Service")
interface CoreResource {

    @PUT
    @Path("process")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun process(@ApiParam("Request", required = true) request: Request): Response
}