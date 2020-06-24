package com.promethist.core.model

import com.promethist.core.Response
import com.promethist.core.model.metrics.Metric
import com.promethist.core.type.*
import org.litote.kmongo.Id
import org.litote.kmongo.newId
import com.promethist.core.model.Message as CoreMessage
import java.util.*

data class Session(
        val _id: Id<Session> = newId(),
        val datetime: Date = Date(),
        val sessionId: String,
        var user: User,
        var application: Application,
        var location: Location? = null,
        val turns: MutableList<Turn> = mutableListOf(),
        val messages: MutableList<Message> = mutableListOf(),
        val metrics: MutableList<Metric> = mutableListOf(),
        val properties: MutablePropertyMap = mutableMapOf(),
        val attributes: Attributes = Attributes(),
        val dialogueStack: DialogueStack = LinkedList()
) {
    val clientType get()  = attributes["client"]?.get("clientType")?.let {
        (it as Memory<String>).value
    } ?: "unknown"

    data class DialogueStackFrame(
            val name: String,
            val buildId: String,
            val args: PropertyMap,
            val nodeId: Int = 0
    )

    data class Message(
            val datetime: Date?,
            val sender: String?,
            val recipient: String?,
            val items: MutableList<Response.Item>
    ) {
        constructor(message: CoreMessage) : this(message.datetime, message.sender, message.recipient, message.items)
    }

    fun addMessage(message: Message) = messages.add(message)
    fun addMessage(message: CoreMessage) = messages.add(Message(message))
}