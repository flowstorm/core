package com.promethist.core.resources

import com.promethist.core.model.Application
import com.promethist.core.model.User
import io.swagger.annotations.Api
import io.swagger.annotations.ApiParam
import io.swagger.annotations.Authorization
import javax.ws.rs.*
import javax.ws.rs.core.MediaType


@Api(tags = ["Content Distribution"], authorizations = [Authorization("Authorization")])
@Path("/contentDistribution")
@Produces(MediaType.APPLICATION_JSON)
interface ContentDistributionResource {

    @GET
    @Path("/{sender}")
    fun resolve(
            @ApiParam(required = true) @PathParam("sender") sender: String
    ): UserContent

    data class UserContent(val user: User, val applications: List<Application>)
}