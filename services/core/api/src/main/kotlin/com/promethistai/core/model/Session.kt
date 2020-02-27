package com.promethistai.core.model

import com.promethistai.port.model.Message.Item
import org.litote.kmongo.Id
import org.litote.kmongo.newId
import java.util.*
import com.promethistai.port.model.Message as PortMessage

data class Session(
        val _id: Id<Session> = newId(),
        val datetime: Date = Date(),
        val sessionId: String,
        val user_id: Id<User>,
        val applicationId: Id<Application>,
        val messages: MutableList<Message> = mutableListOf(),
        val metrics: MutableList<Metric> = mutableListOf(Metric("session", "Count", 1))
) {
    data class Metric(val namespace: String, val name: String, var value: Long = 0) {
        fun increment() = value++
    }

    data class Message(
            val datetime: Date?,
            val sender: String?,
            val recipient: String?,
            val items: MutableList<Item>
    ) {
        constructor(message: PortMessage) : this(message.datetime, message.sender, message.recipient, message.items)
    }

    fun addMessage(message: PortMessage) = messages.add(Message(message))
}