package ai.flowstorm.core.tts

import ai.flowstorm.core.AudioFileType
import java.io.IOException

class TtsAudioFile(private val service: TtsService, val request: TtsRequest, val fileType: AudioFileType) {

    val path: String = "tts/${request.code}.${fileType.name}"
    var data: ByteArray? = null

    /**
     * Returns or generates audio data if not already set.
     */
    fun speak(): TtsAudioFile {
        if (data == null) {
            try {
                data = service.speak(request, fileType)
            } catch (e: Throwable) {
                throw IOException(e.message, e)
            }
        }
        return this
    }
}