package com.promethist.core.dialogue

val BasicDialogue.vocatives: Map<String, Map<String, String>> by lazy {
    mapOf(
            "cs" to String(BasicDialogue::class.java.getResourceAsStream("/dialogue/vocative_cs.txt").readBytes()).trim().split("\n").map {
                it.trim().split(";").let { p ->
                    p[0] to p[1]
                }
            }.toMap()
    )
}

fun BasicDialogue.vocative(name: String): String =
        when (language) {
            "cs" -> vocatives["cs"]!![name] ?: name.replace(Regex("a$"), "o")
                    .replace(Regex("(?<=[cai]el)$"), "i")
                    .replace(Regex("něk$"), "ňku")
                    .replace(Regex("el$"), "le")
                    .replace(Regex("ek$"), "ku")
                    .replace(Regex("ec$"), "če")
                    .replace(Regex("(?<=[td])r$"), "ře")
                    .replace(Regex("(?<=[šjxcsz])$"), "i")
                    .replace(Regex("(?<=[kh])$"), "u")
                    .replace(Regex("(?<=[nvdrlmft])$"), "e")
                    .replace(Regex("(?<=[gkh])$"), "u")
                    .replace(Regex("(?<=[ei]us)$"), "e")
            else -> name
        }

fun BasicDialogue.greeting(name: String? = null) = (
        if (now.hour >= 18 || now.hour < 3)
            mapOf(
                    "en" to "good evening",
                    "de" to "guten abend",
                    "cs" to "dobrý večer",
                    "fr" to "bonsoir"
            )[language] ?: unsupportedLanguage()
        else if (now.hour < 12)
            mapOf(
                    "en" to "good morning",
                    "de" to "guten morgen",
                    "cs" to "dobré ráno",
                    "fr" to "bonjour"
            )[language] ?: unsupportedLanguage()
        else
            mapOf(
                    "en" to "good afternoon",
                    "de" to "guten tag",
                    "cs" to "dobré odpoledne",
                    "fr" to "bonne après-midi"
            )[language] ?: unsupportedLanguage()
        ) + indent(name?.let { vocative(it) }, ", ")

fun BasicDialogue.farewell(name: String? = null) = (
        if (now.hour >= 21 || now.hour < 3)
            mapOf(
                    "en" to "good night",
                    "de" to "gute nacht",
                    "cs" to "dobrou noc",
                    "fr" to "bonne nuit"
            )[language] ?: unsupportedLanguage()
        else
            mapOf(
                    "en" to "good bye",
                    "de" to "auf wiedersehen",
                    "cs" to "nashledanou",
                    "fr" to "au revoir"
            )[language] ?: unsupportedLanguage()
        ) + indent(name?.let { vocative(it) }, ", ")