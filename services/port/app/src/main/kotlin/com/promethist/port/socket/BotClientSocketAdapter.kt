package com.promethist.port.socket

import com.promethist.client.BotConfig
import com.promethist.client.BotEvent
import com.promethist.common.ObjectUtil.defaultMapper
import com.promethist.port.stt.*
import java.io.IOException
import java.nio.ByteBuffer

class BotClientSocketAdapter : AbstractBotSocketAdapter() {

    override lateinit var config: BotConfig
    override lateinit var appKey: String
    override lateinit var sender: String
    override var token: String? = null

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
                is BotEvent.InputAudioStreamOpen -> {
                    stopRecognition(false)
                    startRecognition(SttConfig(locale ?: config.locale, config.zoneId, config.sttSampleRate))
                }
                is BotEvent.InputAudioStreamClose -> { }
                is BotEvent.InputAudioStreamCancel -> stopRecognition(true)
                is BotEvent.SessionEnded -> sendEvent(BotEvent.SessionEnded())
                else -> error("Unexpected event of type ${event::class.simpleName}")
            }
        } catch (e: Throwable) {
            logger.error("onWebSocketText", e)
            (e.cause?:e).apply {
                sendEvent(BotEvent.Error(message?:this::class.qualifiedName?:"unknown"))
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