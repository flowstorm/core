package com.promethist.port.tts

import com.promethist.core.model.Voice
import com.promethist.util.DataConverter
import java.security.MessageDigest

data class TtsRequest(
        val voice: Voice,
        var text: String,
        var isSsml: Boolean = false,
        var style: String = "",
        val sampleRate: Int = 16000,
        var speakingRate: Double = 1.0,
        var speakingPitch: Double = 0.0,
        var speakingVolumeGain: Double = 1.0
) {

    fun code() = DataConverter.digest((text + isSsml + voice + speakingRate + speakingPitch + speakingVolumeGain + style).toByteArray())
}
