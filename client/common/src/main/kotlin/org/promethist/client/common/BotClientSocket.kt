package org.promethist.client.common

import org.promethist.client.BotEvent
import org.promethist.client.BotSocket
import org.promethist.common.ObjectUtil
import org.promethist.util.LoggerDelegate
import java.io.IOException
import java.nio.ByteBuffer

abstract class BotClientSocket(open val url: String, open val raiseExceptions: Boolean = false, open val socketPing: Long = 10): BotSocket {

    companion object {
        const val DEFAULT_URI = "/socket/"
    }

    inner class Watcher : Runnable {

        override fun run() {
            while (state != BotSocket.State.Closed) {
                Thread.sleep(10000)
                if (state == BotSocket.State.Failed)
                    open()
            }
            logger.info("Watcher run end")
        }
    }

    override var state = BotSocket.State.New
    override var listener: BotSocket.Listener? = null

    protected val logger by LoggerDelegate()
    protected var objectMapper = ObjectUtil.defaultMapper

    init {
        Watcher().let { Thread(it).start() }
    }

    abstract fun sendText(text: String)

    abstract fun sendBytes(bytes: ByteBuffer)

    override fun sendEvent(event: BotEvent) {
        if (state != BotSocket.State.Open) {
            val message = "Socket not open for sending event ($event)"
            if (raiseExceptions)
                throw IOException(message)
            logger.warn(message)
        } else {
            logger.info("Sending event ($event)")
            val text = objectMapper.writeValueAsString(event)
            sendText(text)
        }
    }

    override fun sendAudioData(data: ByteArray, count: Int?) {
        if (state != BotSocket.State.Open) {
            val message = "Socket not open for sending binary ${data.size} bytes (count=$count)"
            if (raiseExceptions)
                throw IOException(message)
            logger.warn(message)
        } else {
            logger.debug("Sending binary ${data.size} bytes (count=$count)")
            sendBytes(if (count != null)
                ByteBuffer.wrap(data, 0, count)
            else
                ByteBuffer.wrap(data))
        }
    }

    override fun close() = logger.info("Close")

}