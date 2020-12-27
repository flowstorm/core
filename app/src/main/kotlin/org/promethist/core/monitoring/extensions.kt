package org.promethist.core.monitoring

import org.promethist.common.monitoring.Monitor
import org.promethist.core.model.Session

fun Monitor.capture(e: Throwable, session: Session) = capture(e, mapOf(
    "sessionId" to session.sessionId,
    "applicationName" to session.application.name,
    "dialogue_id" to session.application.dialogue_id.toString(),
    "user_id" to session.user._id.toString()
))