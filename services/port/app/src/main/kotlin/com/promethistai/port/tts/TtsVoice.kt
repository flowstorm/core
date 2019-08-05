package com.promethistai.port.tts

import java.io.Serializable

data class TtsVoice(
    val name: String,
    val gender: String,
    val language: String) : Serializable
