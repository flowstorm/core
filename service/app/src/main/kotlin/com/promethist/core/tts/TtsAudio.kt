package com.promethist.core.tts

import java.io.IOException

class TtsAudio(val ttsRequest: TtsRequest) {

    val code = ttsRequest.code()
    var type = "audio/mpeg"
    var data: ByteArray? = null
    var path: String? = null

    /**
     * Returns or generates audio data if not already set.
     */
    fun speak(): TtsAudio {
        if (data == null) {
            try {
                data = TtsServiceFactory.speak(ttsRequest)
            } catch (e: Throwable) {
                throw IOException(e.message, e)
            }
        }
        return this
    }
}