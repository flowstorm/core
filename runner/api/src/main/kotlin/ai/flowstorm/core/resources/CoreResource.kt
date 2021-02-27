package ai.flowstorm.core.resources

import io.swagger.annotations.Api
import io.swagger.annotations.ApiParam
import ai.flowstorm.core.Request
import ai.flowstorm.core.Response
import ai.flowstorm.security.Authenticated
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