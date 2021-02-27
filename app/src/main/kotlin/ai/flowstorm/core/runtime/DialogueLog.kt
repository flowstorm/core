package ai.flowstorm.core.runtime

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import ai.flowstorm.core.model.LogEntry
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.LocalDateTime
import java.util.*
import kotlin.reflect.KClass

class DialogueLog {

    val log: MutableList<LogEntry> = mutableListOf()
    private val context = LoggerContext()
    private val root = context.getLogger("ROOT")

    val logger: Logger get() = root

    var level
        get() = root.level!!
        set(value) {
            root.level = value
        }

    init {
        with(root) {
            level = Level.ALL
            addAppender(Appender(log).also { it.context = loggerContext; it.start() })
        }
    }

    fun getLogger(loggerName: String): Logger {
        return context.getLogger(loggerName)
    }

    fun getLogger(clazz: KClass<Any>): Logger {
        return getLogger(clazz.qualifiedName!!)
    }

    class Appender(private val log: MutableList<LogEntry>) : AppenderBase<ILoggingEvent>() {
        private val systemLogger = LoggerFactory.getLogger("DialogueLog")
        private val start = System.currentTimeMillis()

        override fun append(e: ILoggingEvent) {
            val time = LocalDateTime.ofInstant(Instant.ofEpochMilli(e.timeStamp), TimeZone.getDefault().toZoneId())
            log.add(LogEntry(
                    time,
                    (e.timeStamp - start).toFloat() / 1000,
                    LogEntry.Level.valueOf(e.level.toString()),
                    e.message))

            when (e.level) {
                Level.TRACE -> systemLogger.trace(e.formattedMessage)
                Level.DEBUG -> systemLogger.debug(e.formattedMessage)
                Level.INFO -> systemLogger.info(e.formattedMessage)
                Level.WARN -> systemLogger.warn(e.formattedMessage)
                Level.ERROR -> systemLogger.error(e.formattedMessage)
            }
        }
    }
}