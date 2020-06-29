package com.promethist.core.model

import org.litote.kmongo.Id
import org.litote.kmongo.newId
import java.util.*

data class DialogueEvent(
        val _id: Id<DialogueEvent> = newId(),
        val datetime: Date = Date(),
        val type: Type,
        val userId: Id<User>?,
        val sessionId: Id<Session>?,
        val applicationName: String,
        val dialogueName: String?,
        val nodeId: Int?,
        val text: String
) {

    enum class Type { ServerError, UserError, UserComment }
}
