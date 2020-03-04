package com.promethist.port.bot

import ai.promethist.client.BotClientRequirements
import ai.promethist.client.BotEvent
import ai.promethist.client.BotSocket
import com.promethist.common.ObjectUtil
import com.promethist.core.model.Message
import com.promethist.core.model.MessageItem
import com.promethist.core.model.TtsConfig
import com.promethist.core.resources.BotService
import com.promethist.port.Application
import com.promethist.port.DataService
import com.promethist.port.stt.*
import com.promethist.port.tts.TtsRequest
import org.eclipse.jetty.websocket.api.WebSocketAdapter
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.ByteBuffer
import java.util.*
import javax.inject.Inject

class BotSocketAdapter : BotSocket, WebSocketAdapter() {

    inner class BotSttCallback(private val event: BotEvent) : SttCallback {

        override fun onResponse(transcript: String, confidence: Float, final: Boolean) {
            try {
                if (final && !inputAudioStreamCancelled) {
                    sendEvent(BotEvent(BotEvent.Type.Recognized, Message(language = language, items = mutableListOf(MessageItem(text = transcript)))))
                    onMessageEvent(event.apply {
                        this.message!!.language = language
                        this.message!!.attributes["portResponseTime"] = System.currentTimeMillis()
                        this.message!!.items = mutableListOf(MessageItem(text = transcript, confidence = confidence.toDouble()))
                    })
                }
            } catch (e: IOException) {
                e.printStackTrace() }
        }

        override fun onError(e: Throwable) {
            e.printStackTrace()
            if (isConnected)
                sendEvent(BotEvent(BotEvent.Type.Error, Message(sender= "google stt",items = mutableListOf(MessageItem(text = e.message?:"")))))
        }

        override fun onOpen() {
            sendEvent(BotEvent(BotEvent.Type.InputAudioStreamOpen))
        }
    }

    inner class Context(val event: BotEvent) {

        val timerTask = object : TimerTask() {
            override fun run() {
                val messages = dataService.popMessages(event.appKey!!, event.message!!.sender!!, 1)
                for (message in messages)
                    sendResponse(event.appKey!!, message)
            }
        }

        init {
            timer.schedule(timerTask, 2000, 2000)
        }
    }

    override var state = BotSocket.State.Open
    override var listener: BotSocket.Listener? = null

    private var logger = LoggerFactory.getLogger(BotSocketAdapter::class.qualifiedName)

    @Inject
    lateinit var botService: BotService

    @Inject
    lateinit var dataService: DataService

    private val objectMapper = ObjectUtil.defaultMapper
    private var sttService: SttService? = null
    private var sttStream: SttStream? = null
    private var clientRequirements: BotClientRequirements = BotClientRequirements()
    private var inputAudioStreamCancelled: Boolean = false
    private var speechProvider: String = "google"
    private var expectedPhrases: List<Message.ExpectedPhrase> = listOf()
    private val timer: Timer = Timer()
    private val contexts = mutableMapOf<String, Context>()
    private var language: Locale? = null

    fun getContext(event: BotEvent) {
        //TODO persist in mongo
        val contextKey = "${event.appKey}/${event.message!!.sender}"
        if (!contexts.containsKey(contextKey)) {
            val context = Context(event)
            contexts.put(contextKey, context)
        }
    }

    override fun onWebSocketBinary(payload: ByteArray, offset: Int, len: Int) {
        logger.debug("onWebSocketBinary(payload[${payload.size}], offset = $offset, len = $len)")
        super.onWebSocketBinary(payload, offset, len)
        sttStream?.write(payload, offset, len)
    }

    /**
     * Determine if the response from botService will be followed by waiting for user input or another message will be sent to botService
     */
    fun onMessageEvent(event: BotEvent) {
        event.message!!.attributes["portResponseTime"] = System.currentTimeMillis()
        val response = botService.message(event.appKey!!, event.message!!)
        if (response != null) {
            expectedPhrases = response.expectedPhrases?: listOf()
            if (response.sessionEnded) {
                sendResponse(event.appKey!!, response)
                sendEvent(BotEvent(BotEvent.Type.SessionEnded))
                inputAudioClose(false)
            }
            // todo will not work correctly before the subdialogs in helena will be implemented
            else {
                sendResponse(event.appKey!!, response) // client will wait for user input
            }
        }
    }

    override fun onWebSocketText(json: String?) {
        super.onWebSocketText(json)
        try {
            val event = objectMapper.readValue(json, BotEvent::class.java)
            if (/*event == null || */event.type == null)
                return

            logger.info("onWebSocketText(event = $event)")

            if (event.message != null) {

                // set session id
                if (event.message!!.sessionId.isNullOrBlank()) {
                    event.message!!.sessionId = Message.createId()
                    sendEvent(BotEvent(BotEvent.Type.SessionStarted, Message(sessionId = event.message!!.sessionId)))
                }

                if (event.appKey != null && event.message!!.sender != null) {
                    getContext(event)
                }
            }
            onEvent(event)
        } catch (e: Exception) {
            e.printStackTrace()
            sendEvent(BotEvent(BotEvent.Type.Error, Message(sender = "port", items = mutableListOf(MessageItem(text = e.message?:"")))))
        }
    }

    fun onEvent(event: BotEvent) {
        language = event.message?.language?:language
        when (event.type) {

            BotEvent.Type.Requirements -> {
                clientRequirements = event.requirements?:BotClientRequirements()
                sendEvent(BotEvent(BotEvent.Type.Requirements, requirements = clientRequirements))
            }

            BotEvent.Type.SessionStarted ->
                sendEvent(BotEvent(BotEvent.Type.SessionStarted,
                        Message(sessionId = event.message?.sessionId?:Message.createId())))

            BotEvent.Type.SessionEnded -> sendEvent(BotEvent(BotEvent.Type.SessionEnded))

            BotEvent.Type.Message -> onMessageEvent(event)

            BotEvent.Type.TextToSpeech -> sendResponse(event.appKey!!, event.message!!.response(event.message!!.items), true)

            BotEvent.Type.InputAudioStreamOpen -> {
                inputAudioClose(false)
                val contract = dataService.getContract(event.appKey!!)
                val language = language?.language?:contract.language
                val sttConfig = SttConfig(language, clientRequirements.sttSampleRate)
                sttService = SttServiceFactory.create(speechProvider, sttConfig, this.expectedPhrases, BotSttCallback(event))
                sttStream = sttService?.createStream()
            }

            BotEvent.Type.InputAudioStreamClose -> inputAudioClose(false)

            BotEvent.Type.InputAudioStreamCancel -> inputAudioClose(true)

            else -> {}
        }
    }

    override fun onWebSocketClose(statusCode: Int, reason: String?) {
        super.onWebSocketClose(statusCode, reason)
        inputAudioClose(false)
        timer.cancel()
    }

    override fun onWebSocketError(cause: Throwable?) {
        super.onWebSocketError(cause)
        inputAudioClose(false)
        timer.cancel()
    }

    override fun open() = logger.info("open()")

    override fun close() = logger.info("close()")

    private fun inputAudioClose(wasCancelled: Boolean) {
        this.inputAudioStreamCancelled = wasCancelled
        inputAudioClose()
    }

    fun inputAudioClose() {
        logger.info("inputAudioClose()")
        sttStream?.close()
        sttStream = null
        sttService?.close()
        sttService = null
    }

    @Synchronized
    @Throws(IOException::class)
    override fun sendEvent(event: BotEvent) {
        logger.info("sendEvent(event = $event)")
        remote.sendString(objectMapper.writeValueAsString(event))
    }

    override fun sendBinaryData(data: ByteArray, count: Int?) {
        logger.info("sendBinaryData(data[${data.size}])")
        remote.sendBytes(ByteBuffer.wrap(data))
    }

    @Throws(IOException::class)
    internal fun sendResponse(appKey: String, response: Message, ttsOnly: Boolean = false) {
        val contract = dataService.getContract(appKey)
        response.expectedPhrases = null
        for (item in response.items) {
            if (item.text.isNullOrBlank()) {
                logger.debug("item.text.isNullOrBlank() == true")
            } else {
                val ttsRequest = TtsRequest(
                        clientRequirements.ttsVoice?:item.ttsVoice?:TtsConfig.defaultVoice(contract.language),
                        (if (item.ssml != null) item.ssml else item.text)?:"",
                        item.ssml != null
                )
                if (clientRequirements.tts != BotClientRequirements.TtsType.None) {
                    val audio = dataService.getTtsAudio(
                            ttsRequest,
                            clientRequirements.tts != BotClientRequirements.TtsType.RequiredLinks,
                            clientRequirements.tts == BotClientRequirements.TtsType.RequiredStreaming
                    )
                    when (clientRequirements.tts) {
                        BotClientRequirements.TtsType.RequiredLinks ->
                            item.audio = Application.filestoreUrl + '/' + audio.path // caller must know port URL therefore URI is enough

                        BotClientRequirements.TtsType.RequiredStreaming ->
                            sendBinaryData(audio.speak().data!!)
                    }
                }
            }
        }
        if (!ttsOnly) {
            if (response.attributes.containsKey("portResponseTime"))
                response.attributes["portResponseTime"] =
                    System.currentTimeMillis() - response.attributes["portResponseTime"] as Long
            sendEvent(BotEvent(BotEvent.Type.Message, response))
        }
    }
}