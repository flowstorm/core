package org.promethist.core.resources

import org.promethist.core.model.Application
import org.promethist.core.model.User
import org.promethist.core.type.MutablePropertyMap
import io.swagger.annotations.Api
import io.swagger.annotations.Authorization
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@Api(tags = ["Content Distribution"], authorizations = [Authorization("Authorization")])
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ContentDistributionResource {

    @POST
    fun resolve(
            contentRequest: ContentRequest
    ): ContentResponse

    data class ContentRequest(
            val sender: String,
            val token: String?,
            val appKey: String,
            val language: String?
    )

    data class ContentResponse(
            val application: Application,
            val user: User,
            val test: Boolean,
            val sessionProperties: MutablePropertyMap
    )
}