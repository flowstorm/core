package com.promethist.port

import ai.promethist.client.BotClientRequirements
import ai.promethist.client.BotEvent
import ai.promethist.client.BotSocket
import com.promethist.common.AppConfig
import com.promethist.common.ObjectUtil
import com.promethist.core.model.Message
import com.promethist.core.model.MessageItem
import com.promethist.core.model.TtsConfig
import com.promethist.core.resources.BotService
import com.promethist.port.stt.*
import com.promethist.port.tts.TtsRequest
import org.eclipse.jetty.websocket.api.WebSocketAdapter
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.ByteBuffer
import java.util.*
import javax.inject.Inject

class BotSocketAdapter : BotSocket, WebSocketAdapter() {

    inner class BotSttCallback() : SttCallback {

        override fun onResponse(transcript: String, confidence: Float, final: Boolean) {
            try {
                if (final && !inputAudioStreamCancelled) {
                    sendEvent(BotEvent.Recognized(transcript))
                    onText(text = transcript, confidence = confidence.toDouble())
                }
            } catch (e: IOException) {
                onError(e)
            }
        }

        override fun onError(e: Throwable) {
            e.printStackTrace()
            if (isConnected)
                sendEvent(BotEvent.Error(e.message?:""))
        }

        override fun onOpen() {
            sendEvent(BotEvent.InputAudioStreamOpen(null))
        }
    }

    @Inject
    lateinit var botService: BotService

    @Inject
    lateinit var dataService: PortService

    override var state = BotSocket.State.Open
    override var listener: BotSocket.Listener? = null
    private lateinit var appKey: String
    private lateinit var clientRequirements: BotClientRequirements
    private val objectMapper = ObjectUtil.defaultMapper
    private var sttService: SttService? = null
    private var sttStream: SttStream? = null
    private var lastMessage: Message? = null
    private var lastMessageTime: Long? = null
    private var inputAudioTime: Long = 0
    private var inputAudioStreamCancelled: Boolean = false
    private var expectedPhrases: List<Message.ExpectedPhrase> = listOf()
    private var logger = LoggerFactory.getLogger(BotSocketAdapter::class.qualifiedName)

    fun onText(text: String, confidence: Double) = lastMessage?.apply {
        val message = Message(
                sender = sender,
                language = language,
                sessionId = sessionId,
                items = mutableListOf(MessageItem(text = text, confidence = confidence))
        )
        onMessageEvent(BotEvent.Message(appKey, message))
    }

    override fun onWebSocketBinary(payload: ByteArray, offset: Int, len: Int) {
        logger.debug("onWebSocketBinary(payload[${payload.size}], offset = $offset, len = $len)")
        if (!inputAudioStreamCancelled) {
            if (inputAudioTime + 10000 < System.currentTimeMillis()) {
                val text = "\$noaudio"
                inputAudioClose(true)
                sendEvent(BotEvent.Recognized(text))
                onText(text, 1.0)
            } else {
                super.onWebSocketBinary(payload, offset, len)
                sttStream?.write(payload, offset, len)
            }
        }
    }

    /**
     * Determine if the response from botService will be followed by waiting for user input or another message will be sent to botService
     */
    private fun onMessageEvent(event: BotEvent.Message) = event.apply {
        val currentTime = System.currentTimeMillis()
        lastMessageTime = currentTime
        lastMessage = message
        if (message.sessionId == null) {
            message.sessionId = Message.createId()
            sendEvent(BotEvent.SessionStarted(message.sessionId!!))
        }
        val response = botService.message(event.appKey, message)
        if (response != null) {
            response.attributes["serviceResponseTime"] = System.currentTimeMillis() - currentTime
            expectedPhrases = response.expectedPhrases?: listOf()
            response.expectedPhrases = null
            if (response.sessionEnded) {
                sendResponse(event.appKey, response)
                sendEvent(BotEvent.SessionEnded())
                inputAudioClose(false)
            }
            // todo will not work correctly before the subdialogs in helena will be implemented
            else {
                sendResponse(event.appKey, response) // client will wait for user input
            }
        }
    }

    override fun onWebSocketText(json: String?) {
        super.onWebSocketText(json)
        try {
            val event = objectMapper.readValue(json, BotEvent::class.java)
            logger.info("onWebSocketText(event = $event)")
            onEvent(event)
        } catch (e: Exception) {
            e.printStackTrace()
            sendEvent(BotEvent.Error(e.message?:e::class.simpleName?:"unknown"))
        }
    }

    private fun onEvent(event: BotEvent) {
        when (event) {
            is BotEvent.Requirements -> {
                appKey = event.appKey
                clientRequirements = event.requirements
                sendEvent(event)
            }
            is BotEvent.SessionEnded -> sendEvent(BotEvent.SessionEnded())
            is BotEvent.Message -> onMessageEvent(event)
            is BotEvent.InputAudioStreamOpen -> {
                inputAudioClose(false)
                val language = lastMessage?.language?.language?:"en"
                val sttConfig = SttConfig(language, clientRequirements.sttSampleRate)
                sttService = SttServiceFactory.create("Google", sttConfig, this.expectedPhrases, BotSttCallback())
                sttStream = sttService?.createStream()
                inputAudioTime = System.currentTimeMillis()
            }
            is BotEvent.InputAudioStreamClose -> inputAudioClose(false)
            is BotEvent.InputAudioStreamCancel -> inputAudioClose(true)
            else -> sendEvent(BotEvent.Error("Unexpected event of type ${event::class.simpleName}"))
        }
    }

    override fun onWebSocketClose(statusCode: Int, reason: String?) {
        super.onWebSocketClose(statusCode, reason)
        inputAudioClose(false)
    }

    override fun onWebSocketError(cause: Throwable?) {
        super.onWebSocketError(cause)
        inputAudioClose(false)
    }

    override fun open() = logger.info("open()")

    override fun close() = logger.info("close()")

    private fun inputAudioClose(wasCancelled: Boolean) {
        this.inputAudioStreamCancelled = wasCancelled
        inputAudioClose()
    }

    private fun inputAudioClose() {
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
        response.expectedPhrases = null
        for (item in response.items) {
            if (item.text.isNullOrBlank()) {
                logger.debug("item.text.isNullOrBlank() == true")
            } else {
                val ttsRequest =
                    TtsRequest(
                        clientRequirements.ttsVoice?:item.ttsVoice?:TtsConfig.defaultVoice("en"),
                        ((if (item.ssml != null) item.ssml else item.text)?:"").replace(Regex("\\$(\\w+)")) {
                            // command processing
                            when (it.groupValues[1]) {
                                "version" -> {
                                    response.language = Locale.ENGLISH
                                    item.text = "Client version ${clientRequirements.clientVersion}, port version ${AppConfig.version}, environment ${AppConfig.instance.get("namespace", "unknown")}."
                                    item.text!!
                                }
                                else -> ""
                            }
                        },
                        item.ssml != null,
                        speakingRate = response.attributes["speakingRate"]?.toString()?.toDoubleOrNull()?:1.0
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
            if (lastMessageTime != null)
                response.attributes["portResponseTime"] = (System.currentTimeMillis() - lastMessageTime!!)
            sendEvent(BotEvent.Message(appKey, response))
        }
    }
}