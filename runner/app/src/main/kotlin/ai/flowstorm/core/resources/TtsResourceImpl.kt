package ai.flowstorm.core.resources

import ai.flowstorm.common.security.Authorized
import ai.flowstorm.core.tts.TtsAudioService
import ai.flowstorm.core.AudioFileType
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
    lateinit var ttsAudioService: TtsAudioService

    override fun synthesize(request: TtsRequest): TtsResponse {
        if (request.text.matches(Regex("<.*[^\\>]>")))
            request.isSsml = true
        val ttsAudioFile = ttsAudioService.get(request, AudioFileType.mp3, asyncSave = false, download = false)
        return TtsResponse(ttsAudioFile.path, ttsAudioFile.fileType.contentType)
    }
}