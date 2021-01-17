package org.promethist.core.tts

import java.io.IOException

class TtsAudio(val request: TtsRequest, val type: String = "audio/mpeg") {

    val path: String = "tts/${request.code}.mp3"
    var data: ByteArray? = null

    /**
     * Returns or generates audio data if not already set.
     */
    fun speak(): TtsAudio {
        if (data == null) {
            try {
                data = TtsServiceFactory.speak(request)
            } catch (e: Throwable) {
                throw IOException(e.message, e)
            }
        }
        return this
    }
}