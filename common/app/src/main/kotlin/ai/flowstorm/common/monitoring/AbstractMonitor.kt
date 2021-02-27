package ai.flowstorm.common.monitoring

import ai.flowstorm.util.LoggerDelegate

abstract class AbstractMonitor : Monitor {

    val logger by LoggerDelegate()

    init {
        logger.info("Monitor created")
    }
}