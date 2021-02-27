package ai.flowstorm.core.model

import java.util.*
import ai.flowstorm.core.model.TtsConfig.Gender

private val en_US = Locale.forLanguageTag("en-US")
private val en_GB = Locale.forLanguageTag("en-GB")
private val cs_CZ = Locale.forLanguageTag("cs-CZ")

enum class Voice(val config: TtsConfig) {

    George(TtsConfig("Google", en_US, Gender.Male, "en-US-Standard-B")),
    Grace(TtsConfig("Google", en_US, Gender.Female, "en-US-Standard-C")),
    Gordon(TtsConfig("Google", en_GB, Gender.Male, "en-GB-Wavenet-B")),
    Gwyneth(TtsConfig("Google", en_GB, Gender.Female, "en-GB-Wavenet-C")),
    Gabriela(TtsConfig("Google", cs_CZ, Gender.Female, "cs-CZ-Standard-A")),
    Anthony(TtsConfig("Amazon", en_US, Gender.Male,"Matthew", "neural")),
    Audrey(TtsConfig("Amazon", en_US, Gender.Female,"Joanna", "neural")),
    Arthur(TtsConfig("Amazon", en_GB, Gender.Male,"Brian", "neural")),
    Amy(TtsConfig("Amazon", en_GB, Gender.Female,"Amy", "neural")),
    Michael(TtsConfig("Microsoft", en_US, Gender.Male, "en-US-GuyNeural")),
    Mary(TtsConfig("Microsoft", en_US, Gender.Female, "en-US-AriaNeural")),
    Milan(TtsConfig("Microsoft", cs_CZ, Gender.Male,"cs-CZ-Jakub"));
    //Victor(TtsConfig("Voicery", en_US, Gender.Male,"steven")),
    //Victoria(TtsConfig("Voicery", en_US, Gender.Female,"katie"))

    companion object {

        private val defaults = mapOf("en" to Audrey, "cs" to Gabriela)
        fun forLanguage(language: String) = defaults[language] ?: defaults["en"]!!
    }
}

