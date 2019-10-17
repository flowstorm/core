package com.promethistai.port.bot

import java.io.Serializable

data class BotClientRequirements(
        var stt: Boolean = false,
        var sttSampleRate: Int = 16000,
        var returnSsml: Boolean = false,
        var tts: TtsType = TtsType.None) : Serializable {

    enum class TtsType {
        None,
        RequiredStreaming,
        RequiredLinks
    }
}