package com.promethistai.port.tts

import java.io.Serializable

open class TtsConfig(
        open var language: String? = null,
        open var gender: Gender? = null,
        open var voice: String? = null,
        open var speakingRate: Double = 1.0, // in the range [0.25, 4.0], 1.0 is the normal native speed
        open var pitch: Double = 0.0, // in the range [-20.0, 20.0]
        open var volumeGain: Double = 0.0, // in the range [-96.0, 16.0]
        open var sampleRate: Int = 16000
) : Serializable {

    enum class Gender {
        Neutral, Male, Female
    }

    companion object {

        val DEFAULT_EN = TtsConfig("en-US", Gender.Female, "en-US-Standard-C")
        val DEFAULT_CS = TtsConfig("cs-CZ", Gender.Female, "cs-CZ-Standard-A")
    }
}