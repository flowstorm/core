package com.promethistai.port.tts

import java.security.MessageDigest

data class TtsRequest(val voice: String, var text: String, var isSsml: Boolean = false, val sampleRate: Int = 16000) {

    fun code(): String {
        val input = text + isSsml + voice
        val hexChars = "0123456789ABCDEF"
        val bytes = MessageDigest.getInstance("SHA-1").digest(input.toByteArray())
        val result = StringBuilder(bytes.size * 2)
        bytes.forEach {
            val i = it.toInt()
            result.append(hexChars[i shr 4 and 0x0f])
            result.append(hexChars[i and 0x0f])
        }
        return result.toString().toLowerCase()
    }
}
