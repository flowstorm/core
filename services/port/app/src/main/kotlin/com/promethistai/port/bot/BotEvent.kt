package com.promethistai.port.bot

import com.promethistai.port.stt.SttConfig
import java.io.Serializable

data class BotEvent(
    var type: Type? = null,
    var text: String? = null,
    var capabilities: BotClientCapabilities? = null,
    var sttConfig: SttConfig? = null,
    var enabled: Boolean? = null) : Serializable {

    enum class Type {

        Text,
        InputAudioStreamOpen,
        InputAudioStreamClose,
        Recognized,
        Error,
        Capabilities,
        SpeechToText

    }
}
