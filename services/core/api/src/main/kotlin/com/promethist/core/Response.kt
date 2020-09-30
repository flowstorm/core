package com.promethist.core

import com.promethist.core.model.LogEntry
import com.promethist.core.model.SttConfig
import com.promethist.core.model.TtsConfig
import com.promethist.core.model.Voice
import com.promethist.core.type.PropertyMap
import java.util.*

class Response(
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
            var voice: Voice? = null,
            var repeatable: Boolean = true
    ) {
        fun text() = text ?: ""
        fun ssml(provider: TtsConfig.Provider) = ssml(ssml ?: text(), provider)
    }

    fun text() = items.joinToString("\n") { it.text ?: "..." }.trim()
    fun ssml(provider: TtsConfig.Provider) =
            ssml(items.joinToString("\n") { it.ssml ?: it.text ?: "" }.trim(), provider)

    companion object {

        fun ssml(ssml: String, provider: TtsConfig.Provider): String {
            val replacedSsml = ssml.replace(Regex("<voice.*?name=\"(.*?)\">(.*)</voice>")) {
                var name = it.groupValues[1]
                for (i in TtsConfig.values.indices) {
                    val config = TtsConfig.values[i]
                    if (name == config.voice.name) {
                        if (config.provider == provider) {
                            name = config.name
                        } else {
                            for (i2 in TtsConfig.values.indices) {
                                val config2 = TtsConfig.values[i2]
                                if (config2.provider == provider && config2.gender == config.gender && config2.locale == config.locale) {
                                    name = if (provider == TtsConfig.Provider.Google) // Google only supports switching gender
                                        config2.gender.name.toLowerCase()
                                    else
                                        config2.name
                                    break
                                }
                            }
                        }
                        break
                    }
                }
                """<voice ${if (provider == TtsConfig.Provider.Google) "gender" else "name"}="$name">${it.groupValues[2]}</voice>"""
            }
            val finalSsml = if (replacedSsml.startsWith("<speak>"))
                replacedSsml
            else
                "<speak>$replacedSsml</speak>"
            return finalSsml
        }
    }
}