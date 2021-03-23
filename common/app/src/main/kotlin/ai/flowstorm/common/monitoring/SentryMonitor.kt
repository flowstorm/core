package ai.flowstorm.common.monitoring

import ai.flowstorm.common.config.ConfigValue
import io.sentry.Sentry
import io.sentry.SentryEvent
import io.sentry.SentryOptions

class SentryMonitor(@ConfigValue("sentry.dsn") val dsn: String) : AbstractMonitor() {

    init {
        val options = SentryOptions()
        options.dsn = dsn
        Sentry.init(options)
    }

    override fun capture(e: Throwable, extras: Map<String, Any?>) {
        try {
            with(SentryEvent()) {
                throwable = e
                setExtras(extras)
                Sentry.captureEvent(this)
            }
        } catch (t: Throwable) {
            logger.error("Sentry monitor error: ${t.message}", t)
        }
    }
}