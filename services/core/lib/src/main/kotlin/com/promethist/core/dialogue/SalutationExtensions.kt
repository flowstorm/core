package com.promethist.core.dialogue

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
        ) + indent(name, ", ")

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
        ) + indent(name, ", ")