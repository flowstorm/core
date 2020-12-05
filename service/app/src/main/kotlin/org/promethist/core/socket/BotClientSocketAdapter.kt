package org.promethist.core.socket

import org.promethist.client.BotConfig
import org.promethist.client.BotEvent
import org.promethist.common.ObjectUtil.defaultMapper
import org.promethist.core.model.SttConfig
import java.io.IOException
import java.nio.ByteBuffer

class BotClientSocketAdapter : AbstractBotSocketAdapter() {

    override lateinit var config: BotConfig
    override lateinit var appKey: String
    override lateinit var sender: String
    override var token: String? = null
    override val sttConfig
        get() = SttConfig(locale ?: config.locale,
                config.zoneId, config.sttSampleRate, SttConfig.Encoding.LINEAR16, config.sttMode)

    override fun onWebSocketText(json: String?) {
        try {
            val event = defaultMapper.readValue(json, BotEvent::class.java)
            logger.info("onWebSocketText(event = $event)")
            when (event) {
                is BotEvent.Init -> {
                    appKey = event.key
                    sender = event.sender
                    token = event.token
                    config = event.config
                    sendEvent(BotEvent.Ready())
                }
                is BotEvent.Request -> onRequest(event.request)
                is BotEvent.InputAudioStreamOpen -> inputAudioStreamOpen()
                is BotEvent.InputAudioStreamClose -> inputAudioStreamClose()
                is BotEvent.SessionEnded -> sendEvent(BotEvent.SessionEnded())
                else -> error("Unexpected event of type ${event::class.simpleName}")
            }
        } catch (e: Throwable) {
            monitor.capture(e)
            logger.error("onWebSocketText", e)
            (e.cause?:e).apply {
                sendEvent(BotEvent.Error(message ?: this::class.qualifiedName ?: "unknown"))
            }
        }
    }

    override fun onWebSocketBinary(payload: ByteArray, offset: Int, length: Int) = onInputAudio(payload, offset, length)

    @Synchronized
    @Throws(IOException::class)
    override fun sendEvent(event: BotEvent) {
        logger.info("sendEvent(event = $event)")
        remote.sendString(defaultMapper.writeValueAsString(event))
    }

    override fun sendAudioData(data: ByteArray, count: Int?) {
        logger.info("sendAudioData(data[${data.size}])")
        remote.sendBytes(ByteBuffer.wrap(data))
    }
}