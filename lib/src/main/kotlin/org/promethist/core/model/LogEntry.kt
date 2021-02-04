package org.promethist.core.model

import java.time.LocalDateTime

data class LogEntry(val time: LocalDateTime, val relativeTime: Float, val level: Level, val text: String) {
    enum class Level { ERROR, WARN, INFO, DEBUG, TRACE }

    override fun toString() = "+${"%.3f".format(relativeTime)}:$level[$text]"
}