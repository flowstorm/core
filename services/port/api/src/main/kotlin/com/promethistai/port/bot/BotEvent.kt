package com.promethistai.port.bot

import com.promethistai.port.model.Message
import com.promethistai.port.stt.SttConfig
import java.io.Serializable

data class BotEvent(
        var type: Type? = null,
        var message: Message? = null,
        var appKey: String? = null,
        var requirements: BotClientRequirements? = null,
        var enabled: Boolean? = null) : Serializable {

    enum class Type {

        Message,
        InputAudioStreamOpen,
        InputAudioStreamClose,
        InputAudioStreamCancel,
        Recognized,
        Error,
        Requirements,
        SpeechToText,
        SessionStarted,
        SessionEnded

    }
}
