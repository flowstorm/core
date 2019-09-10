package com.promethistai.port.stt

import java.io.Serializable

data class SttConfig(var language: String? = null, var sampleRate: Int = 0, var expectedPhrases: List<String> = listOf()): Serializable
