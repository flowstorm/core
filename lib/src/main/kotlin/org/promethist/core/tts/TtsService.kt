package org.promethist.core.tts

interface TtsService {

    fun speak(ttsRequest: TtsRequest): ByteArray
}