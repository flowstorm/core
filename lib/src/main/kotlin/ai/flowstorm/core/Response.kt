package ai.flowstorm.core

import ai.flowstorm.core.model.LogEntry
import ai.flowstorm.core.model.SttConfig
import ai.flowstorm.core.model.TtsConfig
import ai.flowstorm.core.type.PropertyMap
import ai.flowstorm.security.Digest
import java.util.*

data class Response(
        var locale: Locale? = null,
        var items: MutableList<Item>,
        var logs: MutableList<LogEntry> = mutableListOf(),
        val attributes: PropertyMap = mapOf(),
        val sttMode: SttConfig.Mode? = null,
        var expectedPhrases: MutableList<ExpectedPhrase>? = null,
        var sessionEnded: Boolean = false,
        var sleepTimeout: Int = 0,
        val avatarId: String? = null
) : Hashable {
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
    ) : Hashable {
        override fun hash() = Digest.md5("$text$ssml$image$video$audio$code$background")
        fun text() = text ?: ""
    }

    enum class IVA { AmazonAlexa, GoogleAssistant }

    override fun hash() = Digest.md5(items.joinToString { it.hash() })

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