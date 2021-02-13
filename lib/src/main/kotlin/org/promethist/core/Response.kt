package org.promethist.core

import org.promethist.core.model.LogEntry
import org.promethist.core.model.SttConfig
import org.promethist.core.model.TtsConfig
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
    }

    enum class IVA { AmazonAlexa, GoogleAssistant }

    fun text() = items.joinToString("\n") { it.text ?: "..." }.trim()
    fun ssml(iva: IVA) = items.joinToString("\n") {
        val ssml = it.ssml ?: if (it.text != null && !it.text!!.startsWith('#')) it.text!! else ""
        if (ssml.isNotBlank() && it.ttsConfig != null) {
            val ttsConfig = it.ttsConfig!!
            if (iva == IVA.AmazonAlexa && ttsConfig.amazonAlexaVoice != null)
                "<voice name=\"${ttsConfig.amazonAlexaVoice}\">$ssml</voice>"
            else if (iva == IVA.GoogleAssistant && ttsConfig.googleAssistantVoice != null)
                "<voice name=\"${ttsConfig.googleAssistantVoice}\">$ssml</voice>"
            else
                ssml
        } else ssml
    }.trim()
}