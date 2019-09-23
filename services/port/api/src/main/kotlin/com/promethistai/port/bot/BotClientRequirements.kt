package com.promethistai.port.bot

import java.io.Serializable

data class BotClientRequirements(
    var webSTT: Boolean = false,
    var returnSSML: Boolean = false,
    var webTTS: TtsType = TtsType.None) : Serializable {

    enum class TtsType {
        None,
        RequiredStreaming,
        RequiredLinks
    }
}