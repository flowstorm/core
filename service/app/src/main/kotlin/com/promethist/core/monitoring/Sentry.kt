package com.promethist.core.monitoring

import io.sentry.Sentry
import io.sentry.SentryEvent

class Sentry : AbstractMonitor() {

    init {
        Sentry.init()
    }

    override fun capture(e: Throwable, extras: Map<String, Any?>) {
        return with(SentryEvent()) {
            throwable = e
            setExtras(extras)
            Sentry.captureEvent(this)
        }
    }
}