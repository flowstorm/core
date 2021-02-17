package org.promethist.core.resources

import io.swagger.annotations.Api
import io.swagger.annotations.ApiParam
import org.promethist.core.Request
import org.promethist.core.Response
import org.promethist.security.Authenticated
import javax.ws.rs.Consumes
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(description = "Core Service")
interface CoreResource {
    @PUT
    @Path("process")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun process(@ApiParam("Request", required = true) request: Request): Response
}