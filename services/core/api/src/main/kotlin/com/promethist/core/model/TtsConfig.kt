package com.promethist.core.model

import java.util.*

data class TtsConfig(
        val voice: Voice,
        val provider: Provider,
        val locale: Locale,
        val gender: Gender,
        val name: String/*,
        open var speakingRate: Double = 1.0, // in the range [0.25, 4.0], 1.0 is the normal native speed
        open var pitch: Double = 0.0, // in the range [-20.0, 20.0]
        open var volumeGain: Double = 0.0, // in the range [-96.0, 16.0]
        var sampleRate: Int = 16000*/
) {

    enum class Provider { Google, Amazon, Microsoft, Voicery }

    enum class Gender { Male, Female }

    override fun toString(): String = "TtsConfig(voice = $voice, provider = $provider, locale = $locale, gender = $gender, name = $name)"

    companion object {

        val defaultVoices = mapOf("en" to Voice.Grace, "cs" to Voice.Gabriela)
        fun defaultVoice(language: String) = (defaultVoices[language]?:defaultVoices["en"])!!

        val en_US = Locale.forLanguageTag("en-US")
        val en_GB = Locale.forLanguageTag("en-GB")
        val cs_CZ = Locale.forLanguageTag("cs-CZ")
        val values = listOf(
                TtsConfig(Voice.George, Provider.Google, en_US, Gender.Male, "en-US-Standard-B"),
                TtsConfig(Voice.Grace, Provider.Google, en_US, Gender.Female, "en-US-Standard-C"),
                TtsConfig(Voice.Gordon, Provider.Google, en_GB, Gender.Male, "en-GB-Wavenet-B"),
                TtsConfig(Voice.Gwyneth, Provider.Google, en_GB, Gender.Female, "en-GB-Wavenet-C"),
                TtsConfig(Voice.Gabriela, Provider.Google,cs_CZ, Gender.Female, "cs-CZ-Standard-A"),
                TtsConfig(Voice.Anthony, Provider.Amazon, en_US, Gender.Male,"Matthew"),
                TtsConfig(Voice.Audrey, Provider.Amazon, en_US, Gender.Female,"Joanna"),
                TtsConfig(Voice.Arthur, Provider.Amazon, en_GB, Gender.Male,"Brian"),
                TtsConfig(Voice.Amy, Provider.Amazon, en_GB, Gender.Female,"Amy"),
                TtsConfig(Voice.Michael, Provider.Microsoft, en_US, Gender.Male, "en-US-GuyNeural"),
                TtsConfig(Voice.Mary, Provider.Microsoft, en_US, Gender.Female, "en-US-AriaNeural"),
                TtsConfig(Voice.Milan, Provider.Microsoft, cs_CZ, Gender.Male,"cs-CZ-Jakub"),
                TtsConfig(Voice.Victor, Provider.Voicery, en_US, Gender.Male,"steven"),
                TtsConfig(Voice.Victoria, Provider.Voicery, en_US, Gender.Female,"katie")
        )
        fun forVoice(voice: Voice) = values.singleOrNull { it.voice == voice } ?: error("Undefined TTS config for voice $voice")
    }
}
