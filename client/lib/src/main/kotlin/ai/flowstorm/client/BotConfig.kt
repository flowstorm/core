package ai.flowstorm.client

import ai.flowstorm.core.Defaults
import ai.flowstorm.core.model.SttConfig
import ai.flowstorm.core.model.TtsConfig
import ai.flowstorm.core.model.Voice
import ai.flowstorm.core.AudioFileType
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
    var ttsConfig: TtsConfig? = null,
    var ttsFileType: AudioFileType = AudioFileType.mp3,
    var voice: Voice? = null,
    var returnSsml: Boolean = false,
    var silenceTimeout: Long = 5000,
    var test: Boolean = false) : Serializable {

    enum class TtsType {
        None,
        RequiredStreaming,
        RequiredLinks
    }
}