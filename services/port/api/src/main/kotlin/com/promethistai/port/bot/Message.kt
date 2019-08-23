package com.promethistai.port.bot

import java.io.Serializable

data class Message(
        /**
         * Message text. Can contain specific XML tags (which can be interpered or stripped by channel).
         *
         */
        val text: String = "",

        /**
         * Identification of bot service processing message behind port.
         * Client does not set it (it is determined by platform customer contract).
         * Bot service will put its name there (e.g. helena, illusionist, ...)
         */
        val bot: String? = null,

        /**
         * Sending client identification (determined by client application - e.g. device ID, user auth token etc.)
         */
        val sender: String? = null,

        /**
         * Receiver identification. When message send by client, it is optional (can address specific part of bot
         * service, depending on its type).
         * Bot service is using this to identify client (when not set, it can work as broadcast to all clients
         * (if this will be supported by port in the future).
         */
        val recipient: String? = null,

        /**
         * Conversation session identification. Set by bot service.
         */
        val session: String? = null,

        /**
         * Extension properties for message. Determined by bot service and/or client application.
         */
        val extensions: Map<String, Serializable> = mutableMapOf(),

        /**
         * Confidence score. Client usually does not set (if there is human behind ;-)
         */
        val confidence: Double? = 1.0
)