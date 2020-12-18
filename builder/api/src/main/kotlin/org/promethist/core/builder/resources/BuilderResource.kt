package org.promethist.core.builder.resources

import io.swagger.annotations.Api
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@Api(tags = ["Builder"])
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface BuilderResource {

    data class BuilderInfo(val compiler: String)
    data class BuilderResponse(val status: Int)

    @GET
    @Path("/info")
    fun info(): BuilderInfo

    @POST
    @Path("/build")
    fun build(): BuilderResponse
}