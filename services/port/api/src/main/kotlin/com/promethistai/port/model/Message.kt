package com.promethistai.port.model

import java.io.Serializable
import java.util.*

data class Message(

    /**
     * Message id.
     */
    var _id: String? = createId(),

    /**
     * Reference id to previous logically preceded message (question).
     */
    var _ref: String? = null,

    /**
     * Message text. Can contain specific XML tags (which can be interpered or stripped by channel).
     */
    var text: String = "",

    /**
     * Message language.
     */
    var language: Locale = Locale.ENGLISH,

    /**
     * When message was created.
     */
    val datetime: Date? = Date(),

    /**
     * Identification of bot service processing message behind port.
     * Client does not set it (it is determined by platform customer contract).
     * Bot service will put its name there (e.g. helena, illusionist, ...)
     */
    var bot: String? = null,

    /**
     * Platform customer identification (key). Port will set it to client messages according to valid contract.
     */
    var customer: String? = null,

    /**
     * Sending client identification (determined by client application - e.g. device ID, user auth token etc.)
     */
    var sender: String? = null,

    /**
     * Receiver identification. When message send by client, it is optional (can address specific part of bot
     * service, depending on its type).
     * Bot service is using this to identify client (when not set, it can work as broadcast to all clients
     * (if this will be supported by port in the future).
     */
    var recipient: String? = null,

    /**
     * Conversation session identification. Set by bot service. OR context object ????
     */
    var session: String? = null,

    /**
     * Confidence score. Client usually does not set (if there is human behind ;-)
     */
    var confidence: Double? = 1.0,

    /**
     * Extension properties for message. Determined by bot service and/or client application.
     */
    val extensions: Map<String, Serializable> = mutableMapOf()
) {

    fun response(text: String, confidence: Double): Message {
        val sender = this.recipient
        val recipient = this.sender
        return this.copy(_id = createId(), _ref = _id, sender = sender, recipient = recipient, text = text, confidence = confidence, datetime = Date())
    }

    companion object {

        @JvmStatic
        fun createId(): String {
            return UUID.randomUUID().toString()
        }
    }
}