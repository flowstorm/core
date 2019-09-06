package com.promethistai.port.bot

import com.google.gson.GsonBuilder
import com.promethistai.port.DataService
import com.promethistai.port.model.Message
import com.promethistai.port.stt.SttCallback
import com.promethistai.port.stt.SttService
import com.promethistai.port.stt.SttServiceFactory
import com.promethistai.port.stt.SttStream
import com.promethistai.port.tts.TtsServiceFactory
import org.eclipse.jetty.websocket.api.WebSocketAdapter
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.ByteBuffer
//import java.util.*
import javax.inject.Inject

class BotWebSocket : WebSocketAdapter() {

    private var logger = LoggerFactory.getLogger(BotWebSocket::class.java)

    @Inject
    lateinit var botService: BotService

//    @Inject
//    lateinit var dataService: DataService

    private val gson = GsonBuilder().create()
    private var sttService: SttService? = null
    private var sttStream: SttStream? = null
    private var clientCapabilities: BotClientCapabilities = BotClientCapabilities()
    private var clientRequirements: BotClientRequirements = BotClientRequirements()
    private var speechToText: Boolean = true
    private var speechProvider: String = "google"

//    private val timer: Timer = Timer()
//    private var messageFromQueue: Message? = null

//    private fun task(key: String, recipient: String) = object: TimerTask() {
//        override fun run() {
//            messageFromQueue = dataService.popMessages(key, recipient, 0).getOrNull(0)
//            if (messageFromQueue != null) {
//                sendEvent(BotEvent(BotEvent.Type.SessionStarted))
//                sendMessage(messageFromQueue!!)
//            }
//        }
//    }

    override fun onWebSocketBinary(payload: ByteArray, offset: Int, len: Int) {
        super.onWebSocketBinary(payload, offset, len)
        sttStream?.write(payload, offset, len)
    }

    fun logic(event: BotEvent){
        val message = botService.message(event.key!!, event.message!!)
        if (message != null) {
            if (message.extensions.getOrDefault("session_ended", false) as Boolean) {
                sendEvent(BotEvent(BotEvent.Type.SessionEnded))
                close()
            }
            else if (message.extensions.getOrDefault("dialog_ended", false) as Boolean) {
                sendMessage(message)
                logic(event)
            }
            else {
                sendMessage(message)
            }
        }
    }

    override fun onWebSocketText(json: String?) {
        super.onWebSocketText(json)
        try {
            val event = gson.fromJson<Any>(json, BotEvent::class.java) as BotEvent
            if (/*event == null || */event.type == null)
                return

            if (logger.isInfoEnabled)
                logger.info("onWebSocketText event = $event")

            when (event.type) {

                BotEvent.Type.Capabilities -> {
                    clientCapabilities = event.capabilities!!
                    clientRequirements = event.requirements!!

                    sendEvent(BotEvent(BotEvent.Type.Capabilities))
//                    timer.schedule(task(event.key!!, recipient = event.message!!.sender!!), 0, 5000)
                }

                BotEvent.Type.Text -> {
                    logic(event)
                }

                BotEvent.Type.SessionPush -> {
                    val message = botService.message(event.key!!, event.message!!)
                    if (message != null && message.extensions.getOrDefault("force_added", false) as Boolean){
                        sendEvent(BotEvent(BotEvent.Type.SessionStarted))
                        sendMessage(message)
                    }

                }

                BotEvent.Type.InputAudioStreamOpen -> {
                    close()
                    sttService = SttServiceFactory.create(speechProvider, event.sttConfig!!,
                        object : SttCallback {

                            override fun onResponse(transcript: String, confidence: Float, final: Boolean) {
                                try {
                                    if (final) {
                                        sendEvent(BotEvent(BotEvent.Type.Recognized, Message(text = transcript)))
                                        logic(event.apply { this.message!!.text = transcript; this.message!!.confidence=confidence.toDouble() })
                                    }
                                } catch (e: IOException) {
                                    e.printStackTrace()
                                }
                            }

                            override fun onError(e: Throwable) {
                                e.printStackTrace()
                                if (isConnected)
                                    sendEvent(BotEvent(BotEvent.Type.Error, Message(text = e.message?:"")))
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
            sendEvent(BotEvent(BotEvent.Type.Error, Message(text = e.message?:"")))
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
//        timer.cancel()
    }

    @Throws(IOException::class)
    internal fun sendEvent(event: BotEvent) {
        remote.sendString(gson.toJson(event))
    }

    @Throws(IOException::class)
    internal fun sendAudio(text: String, voice: String, lang: String, ssml: Boolean) {
        val stext = if (ssml) text else text.replace(Regex("<.*?>"), "")
        TtsServiceFactory.create(speechProvider).use { service ->
            if (logger.isInfoEnabled)
                logger.info("sendAudio text = $stext, voice = $voice, lang = $lang")
            val audio = service.speak(stext, voice, lang, ssml)
            remote.sendBytes(ByteBuffer.wrap(audio))
        }
    }

    @Throws(IOException::class)
    internal fun sendMessage(message: Message) {
        sendEvent(BotEvent(BotEvent.Type.Text, message))
        if (speechToText && !clientCapabilities.webSpeechSynthesis) {
            sendAudio(message.text, "cs-CZ-Wavenet-A", "cs-CZ", clientRequirements.ssml)    //FIXME
        }
    }

}