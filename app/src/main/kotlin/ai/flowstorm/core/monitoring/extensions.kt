package ai.flowstorm.core.monitoring

import ai.flowstorm.common.monitoring.Monitor
import ai.flowstorm.core.model.Session

fun Monitor.capture(e: Throwable, session: Session) = capture(e, mapOf(
    "sessionId" to session.sessionId,
    "applicationName" to session.application.name,
    "dialogue_id" to session.application.dialogue_id.toString(),
    "user_id" to session.user._id.toString()
))