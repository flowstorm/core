package com.promethist.core.monitoring

import com.promethist.core.model.Session

interface Monitor {
    fun capture(e: Throwable, session: Session)
    fun capture(e: Throwable, extras: Map<String, Any?> = mapOf())
}