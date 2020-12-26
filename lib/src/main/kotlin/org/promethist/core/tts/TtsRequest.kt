package org.promethist.core.tts

import org.promethist.core.model.Voice
import org.promethist.util.DataConverter
import org.promethist.util.Digest

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

    fun code() = Digest.md5((text + isSsml + voice + speakingRate + speakingPitch + speakingVolumeGain + style).toByteArray())
}
