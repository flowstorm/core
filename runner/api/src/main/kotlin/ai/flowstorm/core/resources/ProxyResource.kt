package ai.flowstorm.core.resources

import io.swagger.annotations.Api
import io.swagger.annotations.ApiParam
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Api(description = "Port resource")
interface ProxyResource {

    @GET
    @Path("{spec : .+}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    fun proxyFile(
            @ApiParam(required = true) @PathParam("spec") spec: String
    ): Response
}