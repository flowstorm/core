package com.promethist.core.monitoring

import com.promethist.core.model.Session

abstract class AbstractMonitor : Monitor {
    override fun capture(e: Throwable, session: Session) = capture(e, mapOf(
            "sessionId" to session.sessionId,
            "applicationName" to session.application.name,
            "dialogue_id" to session.application.dialogue_id.toString(),
            "user_id" to session.user._id.toString()
    ))
}