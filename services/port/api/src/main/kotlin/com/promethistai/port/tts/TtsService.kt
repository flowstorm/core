package com.promethistai.port.tts

import java.io.Closeable

interface TtsService: Closeable {

    val voices: List<TtsVoice>

    fun speak(ttsRequest: TtsRequest): ByteArray
}