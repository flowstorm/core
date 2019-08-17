package com.promethistai.port.bot

import com.google.gson.GsonBuilder
import com.promethistai.port.stt.SttCallback
import com.promethistai.port.stt.SttService
import com.promethistai.port.stt.SttServiceFactory
import com.promethistai.port.stt.SttStream
import com.promethistai.port.tts.TtsServiceFactory
import org.eclipse.jetty.websocket.api.WebSocketAdapter
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.ByteBuffer
import javax.inject.Inject

class BotWebSocket : WebSocketAdapter() {

    private var logger = LoggerFactory.getLogger(BotWebSocket::class.java)

    @Inject
    lateinit var botService: BotService

    private val gson = GsonBuilder().create()
    private var sttService: SttService? = null
    private var sttStream: SttStream? = null
    private var clientCapabilities: BotClientCapabilities = BotClientCapabilities()
    private var speechToText: Boolean = true
    private var speechProvider: String = "google"

    override fun onWebSocketBinary(payload: ByteArray, offset: Int, len: Int) {
        super.onWebSocketBinary(payload, offset, len)
        sttStream?.write(payload, offset, len)
    }

    override fun onWebSocketText(json: String?) {
        super.onWebSocketText(json)
        try {
            val event = gson.fromJson<Any>(json, BotEvent::class.java) as BotEvent
            if (/*event == null || */event.type == null)
                return

            if (logger.isInfoEnabled)
                logger.info("event = $event")

            when (event.type) {

                BotEvent.Type.Capabilities -> {
                    clientCapabilities = event.capabilities!!
                    val message = botService.message(event.key!!, Message("\$intro")) // bot introduce
                    if (message != null)
                        sendMessage(message)
                }

                BotEvent.Type.Text -> {
                    val message = botService.message(event.key!!, Message(event.text!!)) //TODO client should send Message instead of text
                    if (message != null)
                        sendMessage(message)
                }

                BotEvent.Type.InputAudioStreamOpen -> {
                    close()
                    sttService = SttServiceFactory.create(speechProvider, event.sttConfig!!,
                        object : SttCallback {

                            override fun onResponse(transcript: String, confidence: Float, final: Boolean) {
                                try {
                                    if (final) {
                                        sendEvent(BotEvent(BotEvent.Type.Recognized, transcript))
                                        val message = botService.message(event.key!!, Message(transcript))
                                        if (message != null)
                                            sendMessage(message)
                                    }
                                } catch (e: IOException) {
                                    e.printStackTrace()
                                }
                            }

                            override fun onError(e: Throwable) {
                                e.printStackTrace()
                                if (isConnected)
                                    sendEvent(BotEvent(BotEvent.Type.Error, e.message))
                            }

                            override fun onOpen() {
                                sendEvent(BotEvent(BotEvent.Type.InputAudioStreamOpen))
                            }
                        }
                    )
                    sttStream = sttService?.createStream()
                }

                BotEvent.Type.InputAudioStreamClose -> close()

                BotEvent.Type.SpeechToText -> speechToText = event.enabled?:false

                else -> {}
            }

        } catch (e: Exception) {
            e.printStackTrace()
            sendEvent(BotEvent(BotEvent.Type.Error, e.message))
        }
    }

    override fun onWebSocketClose(statusCode: Int, reason: String?) {
        super.onWebSocketClose(statusCode, reason)
        close()
    }

    override fun onWebSocketError(cause: Throwable?) {
        super.onWebSocketError(cause)
        close()
    }

    private fun close() {
        sttStream?.close()
        sttStream = null
        sttService?.close()
        sttService = null
    }

    @Throws(IOException::class)
    internal fun sendEvent(event: BotEvent) {
        remote.sendString(gson.toJson(event))
    }

    @Throws(IOException::class)
    internal fun sendAudio(text: String, voice: String, lang: String) {
        TtsServiceFactory.create(speechProvider).use { service ->
            val audio = service.speak(text, voice, lang)
            remote.sendBytes(ByteBuffer.wrap(audio))
        }
    }

    @Throws(IOException::class)
    internal fun sendMessage(message: Message) {
        val text = message.text.trim()
        if (text.isNotEmpty()) {
            sendEvent(BotEvent(BotEvent.Type.Text, text))
            if (speechToText && !clientCapabilities.webSpeechSynthesis) {
                sendAudio(text, "cs-CZ-Wavenet-A", "cs-CZ")    //FIXME
            }
        }
    }

}