package ai.flowstorm.core

import ai.flowstorm.core.model.LogEntry
import ai.flowstorm.core.model.SttConfig
import ai.flowstorm.core.model.TtsConfig
import ai.flowstorm.core.type.PropertyMap
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
    }

    enum class IVA { AmazonAlexa, GoogleAssistant }

    fun text() = items.joinToString("\n") { it.text ?: "..." }.trim()

    fun ssml(iva: IVA) = items.joinToString("\n") {
        val ssml = it.ssml ?: if (it.text != null && !it.text!!.startsWith('#')) it.text!! else ""
        if (ssml.isNotBlank() && it.ttsConfig != null) {
            val ttsConfig = it.ttsConfig!!
            when (iva) {
                IVA.AmazonAlexa -> {
                    when {
                        ttsConfig.amazonAlexa != null ->
                            "<voice name=\"${ttsConfig.amazonAlexa}\">$ssml</voice>"
                        ttsConfig.provider == "Amazon" ->
                            "<voice name=\"${ttsConfig.name}\">$ssml</voice>"
                        else -> ssml
                    }
                }
                IVA.GoogleAssistant ->
                    "<voice gender=\"${ttsConfig.gender.name.toLowerCase()}\" variant=\"${ttsConfig.googleAssistant ?: 1}\">$ssml</voice>"
            }
        } else ssml
    }.trim()
}