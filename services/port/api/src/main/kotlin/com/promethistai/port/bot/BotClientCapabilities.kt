package com.promethistai.port.bot

import java.io.Serializable

data class BotClientCapabilities(
    var webSpeechSynthesis: Boolean = false,
    var webSpeechToText: Boolean = false) : Serializable