package ai.flowstorm.core.resources

import ai.flowstorm.core.tts.TtsRequest
import ai.flowstorm.core.tts.TtsResponse
import io.swagger.annotations.Api
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(description = "TTS resource")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
interface TtsResource {

    @POST
    @Path("synthesize")
    fun synthesize(
        @ApiParam(required = true) request: TtsRequest
    ): TtsResponse
}