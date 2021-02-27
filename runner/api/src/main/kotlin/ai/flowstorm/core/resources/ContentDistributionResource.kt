package ai.flowstorm.core.resources

import io.swagger.annotations.Api
import ai.flowstorm.core.model.Application
import ai.flowstorm.core.model.Device
import ai.flowstorm.core.model.Space
import ai.flowstorm.core.model.User
import ai.flowstorm.core.type.MutablePropertyMap
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