package com.promethist.core.model

import com.promethist.core.Context
import com.promethist.core.dialogue.Dialogue
import com.promethist.core.type.MutablePropertyMap
import org.litote.kmongo.Id
import org.litote.kmongo.newId
import java.util.*

data class DialogueEvent(
        val _id: Id<DialogueEvent> = newId(),
        var datetime: Date = Date(),
        val type: Type,
        val user: User,
        val sessionId: String,
        val applicationName: String,
        val dialogueName: String?,
        val nodeId: Int?,
        val properties: MutablePropertyMap = mutableMapOf(),
        val text: String

) {
    constructor(context: Context, dialogue: Dialogue, type: Type, text: String): this(
            newId(),
            Date(),
            type,
            context.user,
            context.session.sessionId,
            context.application.name,
            context.application.dialogueName,
            context.session.dialogueStack.last().nodeId.let { nodeId ->
                if (nodeId < Dialogue.GENERATED_USER_INPUT_ID) nodeId else dialogue.nodes.last { it is Dialogue.TransitNode && it.next.id == nodeId }.id
            },
            context.session.properties,
            text)

    enum class Type { ServerError, UserError, UserComment }
}
