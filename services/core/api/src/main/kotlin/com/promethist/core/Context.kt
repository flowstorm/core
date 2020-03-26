package com.promethist.core

import com.promethist.core.model.Profile
import com.promethist.core.model.Turn
import com.promethist.core.model.Session
import org.slf4j.Logger

data class Context(val profile: Profile, val session: Session, val turn: Turn, val logger: Logger) {
    // aliases
    val input get() = turn.input
    val user get() = session.user
    val attributes get() = turn.attributes
    val application get() = session.application
    val previousTurns get() = session.turns.reversed()
    val sessionEnded get() = turn.dialogueStack.isEmpty()
}