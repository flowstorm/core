package org.promethist.core.model

import org.promethist.core.Context
import org.promethist.core.dialogue.AbstractDialogue
import org.promethist.core.type.MutablePropertyMap
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
        val dialogue_id: Id<DialogueModel>?,
        val nodeId: Int?,
        val properties: MutablePropertyMap = mutableMapOf(),
        val text: String,
        val space_id: Id<Space> = NullId()
) {
    constructor(context: Context, dialogue: AbstractDialogue, type: Type, text: String): this(
            newId(),
            Date(),
            type,
            context.user,
            context.session.sessionId,
            context.application.name,
            context.application.dialogue_id,
            context.session.dialogueStack.last().nodeId.let { nodeId ->
                if (nodeId < AbstractDialogue.GENERATED_USER_INPUT_ID) nodeId else dialogue.nodes.last { it is AbstractDialogue.TransitNode && it.next.id == nodeId }.id
            },
            context.session.properties,
            text,
            context.session.space_id,
    )

    enum class Type { ServerError, UserError, UserComment }
}
