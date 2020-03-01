package com.promethistai.core.model

import com.promethistai.core.model.Message.Item as MessageItem
import org.litote.kmongo.Id
import org.litote.kmongo.newId
import java.util.*

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
            val items: MutableList<MessageItem>
    ) {
        constructor(message: Message) : this(message.datetime, message.sender, message.recipient, message.items)
    }

    fun addMessage(message: Message) = messages.add(Message(message))
}