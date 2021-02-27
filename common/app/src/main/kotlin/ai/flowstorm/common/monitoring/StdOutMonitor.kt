package ai.flowstorm.common.monitoring

class StdOutMonitor : AbstractMonitor() {
    override fun capture(e: Throwable, extras: Map<String, Any?>) {
        println("Monitoring event: ${e.message}")
        extras.forEach {
            println("${it.key} = ${it.value}")
        }
    }
}