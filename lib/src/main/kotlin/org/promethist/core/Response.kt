package org.promethist.core

import org.promethist.core.model.LogEntry
import org.promethist.core.model.SttConfig
import org.promethist.core.model.TtsConfig
import org.promethist.core.model.Voice
import org.promethist.core.type.PropertyMap
import java.util.*

data class Response(
        var locale: Locale? = null,
        var items: MutableList<Item>,
        var logs: MutableList<LogEntry>,
        val attributes: PropertyMap,
        val sttMode: SttConfig.Mode?,
        var expectedPhrases: MutableList<ExpectedPhrase>?,
        var sessionEnded: Boolean = false,
        var sleepTimeout: Int = 0
) {
    data class Item (

            var text: String? = null,
            var ssml: String? = null,
            var confidence: Double = 1.0,
            var image: String? = null,
            var video: String? = null,
            var audio: String? = null,
            var code: String? = null,
            var background: String? = null,
            var ttsConfig: TtsConfig? = null,
            var repeatable: Boolean = true
    ) {
        fun text() = text ?: ""
        fun ssml(provider: String) = ssml(ssml ?: text(), provider)
    }

    fun text() = items.joinToString("\n") { it.text ?: "..." }.trim()
    fun ssml(provider: String) =
            ssml(items.joinToString("\n") { it.ssml ?: it.text ?: "" }.trim(), provider)

    companion object {

        fun ssml(ssml: String, provider: String): String {
            val voices = Voice.values()
            val replacedSsml = ssml.replace(Regex("<voice.*?name=\"(.*?)\">(.*)</voice>")) {
                var name = it.groupValues[1]
                for (i in voices.indices) {
                    val voice = voices[i]
                    if (name == voice.name) {
                        if (voice.config.provider == provider) {
                            name = voice.name
                        } else {
                            for (i2 in voices.indices) {
                                val config = voices[i2].config
                                if (config.provider == provider && config.gender == config.gender && config.locale == config.locale) {
                                    name = if (provider == "Google") // Google only supports switching gender
                                        config.gender.name.toLowerCase()
                                    else
                                        config.name
                                    break
                                }
                            }
                        }
                        break
                    }
                }
                """<voice ${if (provider == "Google") "gender" else "name"}="$name">${it.groupValues[2]}</voice>"""
            }
            val finalSsml = if (replacedSsml.startsWith("<speak>"))
                replacedSsml
            else
                "<speak>$replacedSsml</speak>"
            return finalSsml
        }
    }
}