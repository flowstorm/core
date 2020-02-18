package com.promethistai.port.tts

data class TtsConfig(
        val voice: String,
        val provider: Provider,
        val language: String,
        val gender: Gender,
        val name: String/*,
        open var speakingRate: Double = 1.0, // in the range [0.25, 4.0], 1.0 is the normal native speed
        open var pitch: Double = 0.0, // in the range [-20.0, 20.0]
        open var volumeGain: Double = 0.0, // in the range [-96.0, 16.0]
        var sampleRate: Int = 16000*/
) {

    enum class Provider { Google, Amazon, Microsoft }

    enum class Gender { Male, Female }

    override fun toString(): String = "TtsConfig(voice = $voice, provider = $provider, language = $language, gender = $gender, name = $name)"

    companion object {

        val defaultVoices = mapOf("en" to "Grace", "cs" to "Gabriela")
        fun defaultVoice(language: String) = (defaultVoices[language]?:defaultVoices["en"])!!

        val values = listOf(
                TtsConfig("George", Provider.Google, "en-US", Gender.Male, "en-US-Standard-B"),
                TtsConfig("Grace", Provider.Google, "en-US", Gender.Female, "en-US-Standard-C"),
                TtsConfig("Gabriela", Provider.Google,"cs-CZ", Gender.Female, "cs-CZ-Standard-A"),
                TtsConfig("Anthony", Provider.Amazon, "en-US", Gender.Male,"Matthew"),
                TtsConfig("Audrey", Provider.Amazon, "en-US", Gender.Female,"Joanna"),
                TtsConfig("Michael", Provider.Microsoft, "en-US", Gender.Male, "en-US-GuyNeural"),
                TtsConfig("Mary", Provider.Microsoft, "en-US", Gender.Female, "en-US-JessaNeural"),
                TtsConfig("Milan", Provider.Microsoft, "cs-CZ", Gender.Male,"cs-CZ-Jakub")
        )
        fun forVoice(voice: String) = values.singleOrNull() { it.voice == voice }?:error("Undefined TTS config for voice $voice")
    }
}
