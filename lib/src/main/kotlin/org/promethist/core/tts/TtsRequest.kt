package org.promethist.core.tts

import org.promethist.core.model.TtsConfig
import org.promethist.security.Digest

data class TtsRequest(
        val config: TtsConfig,
        var text: String,
        var isSsml: Boolean = false,
        var style: String = "",
        var sampleRate: Int = 16000,
        var speakingRate: Double = 1.0,
        var speakingPitch: Double = 0.0,
        var speakingVolumeGain: Double = 1.0
) {
    val code get() = Digest.md5((text + isSsml + config.code + speakingRate + speakingPitch + speakingVolumeGain + style).toByteArray())
}
