package com.promethistai.port.tts

import java.io.Closeable

interface TtsService: Closeable {

    val voices: List<TtsVoice>

    fun speak(text: String, voiceName: String, language: String, isSsml: Boolean = true): ByteArray
}