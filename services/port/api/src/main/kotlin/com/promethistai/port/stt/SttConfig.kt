package com.promethistai.port.stt

import java.io.Serializable

// todo remove lang
data class SttConfig(var language: String? = null, var sampleRate: Int = 0): Serializable
