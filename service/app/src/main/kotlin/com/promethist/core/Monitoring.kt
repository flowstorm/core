package com.promethist.core

import com.promethist.core.model.Session
import io.sentry.Sentry
import io.sentry.SentryEvent

object Monitoring {

    fun init() = Sentry.init()

    fun capture(e: Throwable, session: Session) = capture(e, session?.let {
        mapOf(
            "sessionId" to session.sessionId,
            "applicationName" to session.application.name,
            "dialogue_id" to session.application.dialogue_id.toString(),
            "user_id" to session.user._id.toString()
        )
    })

    fun capture(e: Throwable, extras: Map<String, Any?>? = null) = with(SentryEvent()) {
        throwable = e
        extras?.let { setExtras(extras) }
        Sentry.captureEvent(this)
    }
}