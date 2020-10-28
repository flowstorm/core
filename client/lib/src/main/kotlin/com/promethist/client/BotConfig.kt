package com.promethist.client

import com.promethist.core.Defaults
import com.promethist.core.model.SttConfig
import com.promethist.core.model.Voice
import java.io.Serializable
import java.time.ZoneId
import java.util.*

data class BotConfig(
        var locale: Locale = Defaults.locale,
        var zoneId: ZoneId = Defaults.zoneId,
        var stt: Boolean = false,
        val sttMode: SttConfig.Mode = SttConfig.Mode.SingleUtterance,
        var sttSampleRate: Int = 16000,
        var tts: TtsType = TtsType.None,
        var returnSsml: Boolean = false,
        var voice: Voice? = null,
        var silenceTimeout: Long = 5000,
        var test: Boolean = false) : Serializable {

    enum class TtsType {
        None,
        RequiredStreaming,
        RequiredLinks
    }
}