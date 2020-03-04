package com.promethist.port.tts

interface TtsService {

    fun speak(ttsRequest: TtsRequest): ByteArray
}