package com.promethistai.port.tts

import java.security.MessageDigest

data class TtsRequest(
        var text: String? = null,
        var isSsml: Boolean = false,
        override var language: String? = null,
        override var gender: Gender? = null,
        override var voice: String? = null) : TtsConfig() {

    fun set(config: TtsConfig): TtsRequest {
        language = config.language
        gender = config.gender
        voice = config.voice
        return this
    }

    fun code(): String {
        val input = text + isSsml + language + gender + voice
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

