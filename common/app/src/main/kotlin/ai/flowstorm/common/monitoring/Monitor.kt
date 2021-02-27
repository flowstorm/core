package ai.flowstorm.common.monitoring


interface Monitor {
    fun capture(e: Throwable, extras: Map<String, Any?> = mapOf())
}