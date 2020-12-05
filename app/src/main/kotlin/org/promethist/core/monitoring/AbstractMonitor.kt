package org.promethist.core.monitoring

import org.promethist.core.model.Session
import org.promethist.util.LoggerDelegate

abstract class AbstractMonitor : Monitor {

    val logger by LoggerDelegate()

    init {
        logger.info("Monitor created")
    }

    override fun capture(e: Throwable, session: Session) = capture(e, mapOf(
            "sessionId" to session.sessionId,
            "applicationName" to session.application.name,
            "dialogue_id" to session.application.dialogue_id.toString(),
            "user_id" to session.user._id.toString()
    ))
}