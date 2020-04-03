package com.promethist.core.resources

import com.promethist.core.model.Application
import com.promethist.core.model.SessionProperties
import com.promethist.core.model.User
import io.swagger.annotations.Api
import io.swagger.annotations.Authorization
import javax.ws.rs.*
import javax.ws.rs.core.MediaType


@Api(tags = ["Content Distribution"], authorizations = [Authorization("Authorization")])
@Path("/contentDistribution")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ContentDistributionResource {

    @POST
    fun resolve(
            contentRequest: ContentRequest
    ): ContentResponse

    data class ContentRequest(
            val sender: String,
            val appKey: String,
            val language: String?,
            val starCondition: Application.StartCondition
    )

    data class ContentResponse(val application: Application, val user: User, val sessionProperties: SessionProperties)
}