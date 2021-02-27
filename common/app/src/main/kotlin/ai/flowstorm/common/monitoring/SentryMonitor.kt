package ai.flowstorm.common.monitoring

import io.sentry.Sentry
import io.sentry.SentryEvent

class SentryMonitor : AbstractMonitor() {

    private var initialized = false

    override fun capture(e: Throwable, extras: Map<String, Any?>) {
        try {
            if (!initialized) {
                initialized = true
                Sentry.init()
            }
            with(SentryEvent()) {
                throwable = e
                setExtras(extras)
                Sentry.captureEvent(this)
            }
        } catch (t: Throwable) {
            logger.error("Monitoring error ${t.message}", t)
        }
    }
}