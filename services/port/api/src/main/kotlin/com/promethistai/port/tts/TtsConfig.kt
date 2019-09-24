package com.promethistai.port.tts

import java.io.Serializable

open class TtsConfig(
        open var language: String? = null,
        open var gender: Gender? = null,
        open var voice: String? = null) : Serializable {

    enum class Gender {
        Neutral, Male, Female
    }

    companion object {

        val DEFAULT_EN = TtsConfig("en-US", Gender.Female, "en-US-Standard-C")
        val DEFAULT_CS = TtsConfig("cs-CZ", Gender.Female, "cs-CZ-Standard-A")
    }
}