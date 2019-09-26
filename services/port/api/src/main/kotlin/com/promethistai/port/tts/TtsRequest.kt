package com.promethistai.port.tts

import java.security.MessageDigest

data class TtsRequest(
        var text: String? = null,
        var isSsml: Boolean = false,
        override var language: String? = null,
        override var gender: Gender? = null,
        override var voice: String? = null,
        override var speakingRate: Double = 1.0,
        override var pitch: Double = 0.0,
        override var volumeGain: Double = 0.0) : TtsConfig() {

    fun set(config: TtsConfig): TtsRequest {
        language = config.language
        gender = config.gender
        voice = config.voice
        return this
    }

    fun code(): String {
        val input = text + isSsml + language + gender + voice + speakingRate + pitch + volumeGain
        val hexChars = "0123456789ABCDEF"
        val bytes = MessageDigest.getInstance("SHA-1").digest(input.toByteArray())
        val result = StringBuilder(bytes.size * 2)
        bytes.forEach {
            val i = it.toInt()
            result.append(hexChars[i shr 4 and 0x0f])
            result.append(hexChars[i and 0x0f])
        }
        return result.toString()
    }
}
