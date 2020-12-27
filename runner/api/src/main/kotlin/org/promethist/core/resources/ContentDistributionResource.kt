package org.promethist.core.resources

import io.swagger.annotations.Api
import org.promethist.core.model.Application
import org.promethist.core.model.Device
import org.promethist.core.model.Space
import org.promethist.core.model.User
import org.promethist.core.type.MutablePropertyMap
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["Content Distribution"])
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ContentDistributionResource {

    data class ContentRequest(
            val deviceId: String,
            val token: String?,
            val appKey: String,
            val language: String?
    )

    data class ContentResponse(
            val application: Application,
            val space: Space,
            val device: Device,
            val user: User,
            val test: Boolean,
            val sessionProperties: MutablePropertyMap
    )

    @POST
    fun resolve(contentRequest: ContentRequest): ContentResponse
}