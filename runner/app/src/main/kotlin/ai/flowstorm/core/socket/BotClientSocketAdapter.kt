package ai.flowstorm.core.socket

import ai.flowstorm.client.BotConfig
import ai.flowstorm.client.BotEvent
import ai.flowstorm.common.ObjectUtil.defaultMapper
import ai.flowstorm.core.model.SttConfig
import java.io.IOException
import java.nio.ByteBuffer

class BotClientSocketAdapter : AbstractBotSocketAdapter() {

    override lateinit var config: BotConfig
    override lateinit var appKey: String
    override lateinit var deviceId: String
    override var token: String? = null
    override val sttConfig
        get() = SttConfig(locale ?: config.locale,
                config.zoneId, config.sttSampleRate, SttConfig.Encoding.LINEAR16, config.sttMode)

    override fun onWebSocketText(json: String?) {
        try {
            val event = defaultMapper.readValue(json, BotEvent::class.java)
            logger.info("Receiving event $event")
            when (event) {
                is BotEvent.Init -> {
                    appKey = event.key
                    deviceId = event.deviceId
                    token = event.token
                    config = event.config
                    sendEvent(BotEvent.Ready())
                }
                is BotEvent.Request -> onRequest(event.request)
                is BotEvent.InputAudioStreamOpen -> inputAudioStreamOpen(event.sessionId)
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
        logger.info("Sending event $event")
        remote.sendString(defaultMapper.writeValueAsString(event))
    }

    override fun sendAudioData(data: ByteArray, count: Int?) {
        logger.info("Sending audio ${data.size} bytes")
        remote.sendBytes(ByteBuffer.wrap(data))
    }
}