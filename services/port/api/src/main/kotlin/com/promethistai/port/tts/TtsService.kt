package com.promethistai.port.tts

interface TtsService {

    fun speak(ttsRequest: TtsRequest): ByteArray
}