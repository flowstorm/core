package com.promethist.core.model

data class MessageItem (
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