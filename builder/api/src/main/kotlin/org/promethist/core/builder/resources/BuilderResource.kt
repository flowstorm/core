package org.promethist.core.builder.resources

import io.swagger.annotations.Api
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@Api(tags = ["Builder"])
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface BuilderResource {

    data class BuildResponse(val status: Int)

    @POST
    @Path("/build")
    fun build(): BuildResponse
}