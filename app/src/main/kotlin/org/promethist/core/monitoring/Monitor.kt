package org.promethist.core.monitoring

import org.promethist.core.model.Session

interface Monitor {
    fun capture(e: Throwable, session: Session)
    fun capture(e: Throwable, extras: Map<String, Any?> = mapOf())
}