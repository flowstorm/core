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
import java.util.*
import javax.inject.Inject

class BotWebSocket : WebSocketAdapter() {

    private var logger = LoggerFactory.getLogger(BotWebSocket::class.java)

    @Inject
    lateinit var botService: BotService

    @Inject
    lateinit var dataService: DataService

    private val gson = GsonBuilder().create()
    private var sttService: SttService? = null
    private var sttStream: SttStream? = null
    private var clientCapabilities: BotClientCapabilities = BotClientCapabilities()
    private var clientRequirements: BotClientRequirements = BotClientRequirements()
    private var speechToText: Boolean = true
    private var inputAudioStreamCancelled: Boolean = false
    private var speechProvider: String = "google"
    private var expectedPhrases: List<String>? = null
    private val timer: Timer = Timer()
    private val timerTasks = mutableMapOf<String, TimerTask>()

    override fun onWebSocketBinary(payload: ByteArray, offset: Int, len: Int) {
        super.onWebSocketBinary(payload, offset, len)
        sttStream?.write(payload, offset, len)
    }

    /**
     * Determine if the response from botService will be followed by waiting for user input or another message will be sent to botService
     */
    fun responseLogic(event: BotEvent) {
        val message = botService.message(event.appKey!!, event.message!!. apply {
            this.extensions["ssml"] = this@BotWebSocket.clientRequirements.ssml;
            this.extensions["expected_phrases"] = !this@BotWebSocket.clientCapabilities.webSpeechToText
        })
        if (message != null) {
            expectedPhrases = message.extensions.getOrDefault("expected_phrases", null) as? List<String>?
            if (message.extensions.getOrDefault("session_ended", false) as Boolean) {
                sendEvent(BotEvent(BotEvent.Type.SessionEnded))
                close(false)
            }
            else if (message.extensions.getOrDefault("dialog_ended", false) as Boolean) {
                sendMessage(message)
                responseLogic(event) // client will be fed with next message
            }
            else {
                sendMessage(message) // client will wait for user input
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

            if (event.message != null) {

                // set session id
                if (event.message!!.session.isNullOrBlank())
                    event.message!!.session = Message.createId()

                if (event.appKey != null && event.message!!.sender != null) {

                    val timerTaskKey = "${event.appKey}/${event.message!!.sender}"
                    if (!timerTasks.containsKey(timerTaskKey)) {
                        val timerTask = object : TimerTask() {
                            override fun run() {
                                val message = dataService.popMessages(event.appKey!!, event.message!!.sender!!, 1).getOrNull(0)
                                if (message != null) {
                                    //sendEvent(BotEvent(BotEvent.Type.SessionStarted))
                                    sendMessage(message)
                                }
                            }
                        }
                        timer.schedule(timerTask, 2000, 2000)
                        timerTasks.put(timerTaskKey, timerTask)
                    }
                }
            }

            when (event.type) {

                BotEvent.Type.Capabilities -> {
                    clientCapabilities = event.capabilities!!
                    clientRequirements = event.requirements?:BotClientRequirements(false)
                    sendEvent(BotEvent(BotEvent.Type.Capabilities))
                }

                BotEvent.Type.SessionStarted -> {
                    sendEvent(BotEvent(BotEvent.Type.SessionStarted, Message(session = Message.createId())))
                }

                BotEvent.Type.SessionEnded -> {
                    sendEvent(BotEvent(BotEvent.Type.SessionEnded))
                }

                BotEvent.Type.Text -> {
                    responseLogic(event)
                }


                BotEvent.Type.InputAudioStreamOpen -> {
                    close(false)
                    sttService = SttServiceFactory.create(speechProvider, event.sttConfig!!.apply {
                        this.expectedPhrases = this@BotWebSocket.expectedPhrases ?: listOf() },
                        object : SttCallback {

                            override fun onResponse(transcript: String, confidence: Float, final: Boolean) {
                                try {
                                    if (final && !inputAudioStreamCancelled) {
                                        sendEvent(BotEvent(BotEvent.Type.Recognized, Message(text = transcript)))
                                        responseLogic(event.apply {
                                            this.message!!.text = transcript
                                            this.message!!.confidence=confidence.toDouble()
                                        })
                                    }
                                } catch (e: IOException) {
                                    e.printStackTrace()
                                }
                            }

                            override fun onError(e: Throwable) {
                                e.printStackTrace()
                                if (isConnected)
                                    sendEvent(BotEvent(BotEvent.Type.Error, Message(sender= "google stt",text = e.message?:"")))
                            }

                            override fun onOpen() {
                                sendEvent(BotEvent(BotEvent.Type.InputAudioStreamOpen))
                            }
                        }
                    )
                    sttStream = sttService?.createStream()
                }

                BotEvent.Type.InputAudioStreamClose -> close(false)

                BotEvent.Type.InputAudioStreamCancel -> close(true)

                BotEvent.Type.SpeechToText -> speechToText = event.enabled?:false

                else -> {}
            }

        } catch (e: Exception) {
            e.printStackTrace()
            sendEvent(BotEvent(BotEvent.Type.Error, Message(sender = "port", text = e.message?:"")))
        }
    }

    override fun onWebSocketClose(statusCode: Int, reason: String?) {
        super.onWebSocketClose(statusCode, reason)
        close( false)
        timer.cancel()
    }

    override fun onWebSocketError(cause: Throwable?) {
        super.onWebSocketError(cause)
        close(false)
        timer.cancel()
    }

    private fun close(wasCancelled: Boolean) {
        this.inputAudioStreamCancelled = wasCancelled
        sttStream?.close()
        sttStream = null
        sttService?.close()
        sttService = null
    }

    @Synchronized
    @Throws(IOException::class)
    internal fun sendEvent(event: BotEvent) {
        remote.sendString(gson.toJson(event))
    }

    @Throws(IOException::class)
    internal fun sendAudio(text: String, voice: String, lang: String, ssml: Boolean) {
        val stext = if (ssml) text else text.replace(Regex("<.*?>"), "")
        TtsServiceFactory.create(speechProvider).use { service ->
            if (logger.isInfoEnabled)
                logger.info("sendAudio text = $stext, voice = $voice, lang = $lang, ssml= $ssml")
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