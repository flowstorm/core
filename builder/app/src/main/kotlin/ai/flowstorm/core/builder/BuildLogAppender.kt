package ai.flowstorm.core.builder

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

class BuildLogAppender : AppenderBase<ILoggingEvent>() {

    override fun append(event: ILoggingEvent) {
        eventMap.getOrPut(event.threadName) { mutableListOf() }.add(event)
    }

    companion object {
        private val eventMap: ConcurrentMap<String, MutableList<ILoggingEvent>> = ConcurrentHashMap()

        fun getEvents(threadName: String): List<String> {
            val start = eventMap[threadName]!!.first().timeStamp
            return eventMap[threadName]!!.map {
                val time = "%.2f".format((it.timeStamp - start).toFloat() / 1000)
                "[+$time ${it.level}] ${it.message}"
            }
        }

        fun clearEvents(threadName: String) {
            eventMap.remove(threadName)
        }
    }
}