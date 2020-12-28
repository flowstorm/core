package org.promethist.common.monitoring

import org.promethist.util.LoggerDelegate

abstract class AbstractMonitor : Monitor {

    val logger by LoggerDelegate()

    init {
        logger.info("Monitor created")
    }
}