package com.promethist.core.nlp

import com.promethist.core.model.Profile
import com.promethist.core.model.Turn
import com.promethist.core.model.Session
import org.slf4j.Logger

data class Context(val profile: Profile, val session: Session, val turn: Turn, val logger: Logger, var sessionEnded: Boolean = false) {
    // aliases
    val input get() = turn.input
    val user get() = session.user
    val attributes get() = turn.attributes
    val application get() = session.application
}