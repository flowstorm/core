package com.promethist.core

import com.promethist.core.model.LogEntry

//TODO after removing Message class, change atrributes type to MutableMap<String, Any>
open class Response(
        open var items: MutableList<Item>,
        open var logs: MutableList<LogEntry>,
        open val attributes: MutableMap<String, *>, //TODO after removing Message class, change atrributes type to MutableMap<String, Any>
        open var expectedPhrases: MutableList<ExpectedPhrase>?,
        open var sessionEnded: Boolean = false
) {

    data class Item (
            /**
             * Message text. Can contain specific XML tags (which can be interpered or stripped by channel).
             */
            var text: String? = null,

            /**
             * Ssml text - Google based by default. Can contain specific XML tags
             */
            var ssml: String? = null,

            /**
             * Confidence score. Client usually does not set (if there is human behind ;-)
             */
            var confidence: Double = 1.0,

            /**
             * Resource links.
             */
            var image: String? = null,
            var video: String? = null,
            var audio: String? = null,

            /**
             * TTS voice
             */
            var ttsVoice: String? = null
    )
}