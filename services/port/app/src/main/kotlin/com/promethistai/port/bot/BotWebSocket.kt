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
    private var clientRequirements: BotClientRequirements = BotClientRequirements()
    private var inputAudioStreamCancelled: Boolean = false
    private var speechProvider: String = "google"
    private var expectedPhrases: List<Message.ExpectedPhrase>? = null
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
        val messages = botService.message(event.appKey!!, event.message!!)
        if (messages != null) {
            expectedPhrases = messages.expectedPhrases
            if (messages.sessionEnded == true) {
                sendEvent(BotEvent(BotEvent.Type.SessionEnded))
                close(false)
            }
            else {
                sendMessage(messages) // client will wait for user input
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
                if (event.message!!.session.isNullOrBlank()) {
                    event.message!!.session = Message.createId()
                    sendEvent(BotEvent(BotEvent.Type.SessionStarted, Message(session = event.message!!.session)))
                }

                if (event.appKey != null && event.message!!.sender != null) {

                    val timerTaskKey = "${event.appKey}/${event.message!!.sender}"
                    if (!timerTasks.containsKey(timerTaskKey)) {
                        val timerTask = object : TimerTask() {
                            override fun run() {
                                val messages = dataService.popMessages(event.appKey!!, event.message!!.sender!!, 1)
                                for (message in messages)
                                    sendMessage(message)
                            }
                        }
                        timer.schedule(timerTask, 2000, 2000)
                        timerTasks.put(timerTaskKey, timerTask)
                    }
                }
            }

            when (event.type) {

                BotEvent.Type.Capabilities -> {
                    clientRequirements = event.requirements?:BotClientRequirements()
                    sendEvent(BotEvent(BotEvent.Type.Capabilities))
                }

                BotEvent.Type.SessionStarted -> {
                    sendEvent(BotEvent(BotEvent.Type.SessionStarted, Message(session = event.message?.session?:Message.createId())))
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
                                        sendEvent(BotEvent(BotEvent.Type.Recognized, Message(items = mutableListOf(Message.Item(text = transcript)))))
                                        responseLogic(event.apply {
                                            this.message!!.items = mutableListOf(Message.Item(text = transcript, confidence = confidence.toDouble()))
                                        })
                                    }
                                } catch (e: IOException) {
                                    e.printStackTrace()
                                }
                            }

                            override fun onError(e: Throwable) {
                                e.printStackTrace()
                                if (isConnected)
                                    sendEvent(BotEvent(BotEvent.Type.Error, Message(sender= "google stt",items = mutableListOf(Message.Item(text = e.message?:"")))))
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

                else -> {}
            }

        } catch (e: Exception) {
            e.printStackTrace()
            sendEvent(BotEvent(BotEvent.Type.Error, Message(sender = "port", items = mutableListOf(Message.Item(text = e.message?:"")))))
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
    internal fun saveAudio(text: String, voice: String, lang: String, ssml: Boolean) : String {
        val stext = if (ssml) text else text.replace(Regex("<.*?>"), "")
        TtsServiceFactory.create(speechProvider).use { service ->
            if (logger.isInfoEnabled)
                logger.info("sendAudio text = $stext, voice = $voice, lang = $lang, ssml= $ssml")
            val audio = service.speak(stext, voice, lang, ssml)
            // todo save bytes: ByteBuffer.wrap(audio), https://promethistai.atlassian.net/browse/AIP-8
            return "To be implemented"
        }
    }

    @Throws(IOException::class)
    internal fun sendMessage(message: Message) {
        message.expectedPhrases = null
        for (item in message.items) {
            if (clientRequirements.webTTS == BotClientRequirements.TtsType.RequiredStreaming) {
                sendAudio(text = item.links.first { s -> s.type == "ssml" }.ref!!,
                        voice = (item.extensions["voice"] as? String) ?: "cs-CZ-Wavenet-A",
                        lang = (item.extensions["lang"] as? String) ?: "cs-CZ",
                        ssml = true)
            } else if (clientRequirements.webTTS == BotClientRequirements.TtsType.RequiredLinks) {
                item.links.add(message.links.lastIndex,
                                    Message.ResourceLink(type = "audio",
                                                        ref =  saveAudio(text = item.text?:"",
                                                                        voice = (item.extensions["voice"] as? String) ?: "cs-CZ-Wavenet-A",
                                                                        lang = (item.extensions["lang"] as? String) ?: "cs-CZ",
                                                                        ssml = true)))
            }
        }
        sendEvent(BotEvent(BotEvent.Type.Text, message))
    }

}