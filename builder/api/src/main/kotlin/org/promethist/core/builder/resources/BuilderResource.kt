package org.promethist.core.builder.resources

import io.swagger.annotations.Api
import io.swagger.annotations.ApiParam
import org.promethist.core.builder.Info
import org.promethist.core.builder.Request
import org.promethist.core.builder.Response
import org.promethist.security.Authenticated
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@Api(tags = ["Builder"])
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface BuilderResource {

    @GET
    @Path("/info")
    fun info(): Info

    @POST
    @Path("/build")
    @Authenticated
    fun build(@ApiParam("Request", required = true) request: Request): Response
}