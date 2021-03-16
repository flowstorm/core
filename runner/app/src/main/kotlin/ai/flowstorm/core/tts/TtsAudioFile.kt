package ai.flowstorm.core.tts

import java.io.IOException

class TtsAudioFile(private val service: TtsService, val request: TtsRequest, val type: String = "audio/mpeg") {

    val path: String = "tts/${request.code}.mp3"
    var data: ByteArray? = null

    /**
     * Returns or generates audio data if not already set.
     */
    fun speak(): TtsAudioFile {
        if (data == null) {
            try {
                data = service.speak(request)
            } catch (e: Throwable) {
                throw IOException(e.message, e)
            }
        }
        return this
    }
}