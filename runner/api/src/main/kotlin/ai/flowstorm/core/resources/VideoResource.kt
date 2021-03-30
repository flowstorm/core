package ai.flowstorm.core.resources

import io.swagger.annotations.Api
import io.swagger.annotations.ApiParam
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@Api(tags = ["Video"])
@Consumes(MediaType.APPLICATION_JSON)
interface VideoResource {

    @GET
    @Path("{deviceId}/playlist.m3u8")
    //@Produces("application/x-mpegurl")
    @Produces(MediaType.TEXT_PLAIN)
    fun mediaPlaylist(
        @ApiParam(required = true) @PathParam("deviceId") deviceId: String
    ): String
}