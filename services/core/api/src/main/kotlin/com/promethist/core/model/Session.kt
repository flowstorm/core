package com.promethist.core.model

import com.promethist.core.model.metrics.Metric
import com.promethist.core.type.*
import org.litote.kmongo.Id
import org.litote.kmongo.newId
import java.util.*

data class  Session(
        override val _id: Id<Session> = newId(),
        val datetime: Date = Date(),
        val sessionId: String,
        val test: Boolean = false,
        var user: User,
        var application: Application,
        var location: Location? = null,
        val initiationId: String? = null,
        val turns: MutableList<Turn> = mutableListOf(),
        val metrics: MutableList<Metric> = mutableListOf(),
        val properties: MutablePropertyMap = mutableMapOf(),
        val attributes: Attributes = Attributes(),
        val analytics: Dynamic = Dynamic(),
        val dialogueStack: DialogueStack = LinkedList()
) : Entity<Session> {
    val isInitiated get() = initiationId != null
    val clientType get()  = attributes[DialogueModel.defaultNamespace]?.get("clientType")?.let {
        (it as Memory<String>).value
    } ?: "unknown"

    data class DialogueStackFrame(
            val id: String? = null, // nullable to be able to load older sessions
            val buildId: String,
            val args: PropertyMap,
            val nodeId: Int = 0,
            val name: String? = null
    )
}