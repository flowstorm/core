package com.promethistai.port.stt

import com.promethistai.port.model.Message
import java.io.Serializable

data class SttConfig(var language: String? = null, var sampleRate: Int = 0, var expectedPhrases: List<Message.ExpectedPhrase> = listOf()): Serializable
