package com.promethist.core.tts

interface TtsService {

    fun speak(ttsRequest: TtsRequest): ByteArray
}