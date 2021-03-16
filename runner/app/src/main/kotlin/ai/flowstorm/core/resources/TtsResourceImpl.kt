package ai.flowstorm.core.resources

import ai.flowstorm.common.security.Authorized
import ai.flowstorm.core.tts.TtsAudioFileService
import ai.flowstorm.core.tts.TtsRequest
import ai.flowstorm.core.tts.TtsResponse
import javax.inject.Inject
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("/tts")
@Produces(MediaType.APPLICATION_JSON)
@Authorized
class TtsResourceImpl : TtsResource {

    @Inject
    lateinit var ttsAudioFileService: TtsAudioFileService

    override fun synthesize(request: TtsRequest): TtsResponse {
        val ttsAudioFile = ttsAudioFileService.get(request, asyncSave = false, download = false)
        return TtsResponse(ttsAudioFile.path, ttsAudioFile.type)
    }
}