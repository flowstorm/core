package com.promethistai.port.tts

import java.io.Serializable

data class TtsRequest(
        var text: String? = null,
        var language: String? = null,
        var gender: Gender? = null,
        var voice: String? = null) : Serializable {

    enum class Gender {
        Neutral, Male, Female
    }

}
