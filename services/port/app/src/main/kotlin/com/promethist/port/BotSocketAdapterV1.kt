package com.promethist.port

import ai.promethist.client.BotClientRequirements
import ai.promethist.client.BotEvent
import ai.promethist.client.BotSocket
import com.promethist.common.AppConfig
import com.promethist.common.ObjectUtil
import com.promethist.common.ServiceUrlResolver
import com.promethist.core.*
import com.promethist.core.model.Message
import com.promethist.core.model.TtsConfig
import com.promethist.core.resources.CoreResource
import com.promethist.core.type.MutablePropertyMap
import com.promethist.port.stt.*
import com.promethist.port.tts.TtsRequest
import org.eclipse.jetty.websocket.api.WebSocketAdapter
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.ByteBuffer
import java.util.*
import javax.inject.Inject

class BotSocketAdapterV1 : BotSocket, WebSocketAdapter() {

    inner class BotSttCallback() : SttCallback {

        override fun onResponse(input: Input, final: Boolean) {
            try {
                if (final && !inputAudioStreamCancelled) {
                    sendEvent(BotEvent.Recognized(input.transcript.text))
                    onInput(input)
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
            sendEvent(BotEvent.InputAudioStreamOpen())
        }
    }

    @Inject
    lateinit var coreResource: CoreResource

    @Inject
    lateinit var dataService: PortService

    override var listener: BotSocket.Listener? = null

    override var state = BotSocket.State.Open

    private lateinit var appKey: String
    private lateinit var sender: String
    private var sessionId: String? = null
    private var language: Locale? = null // deprecated - version 1 only, remove in version 3
    private lateinit var clientRequirements: BotClientRequirements

    // STT
    private var inputAudioTime: Long = 0
    private var inputAudioStreamCancelled: Boolean = false
    private var sttService: SttService? = null
    private var sttStream: SttStream? = null
    private var expectedPhrases: List<ExpectedPhrase> = listOf()

    //private var lastMessage: Message? = null
    private var lastMessageTime: Long? = null

    private var logger = LoggerFactory.getLogger(BotSocketAdapterV1::class.qualifiedName)


    override fun open() {
        logger.info("open()")
        //TODO create connection object
    }

    override fun close() {
        logger.info("close()")
        //TODO delete connection object
    }

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

    private fun isDetectingAudio(payload: ByteArray, offset: Int, length: Int): Boolean {
        for (i in offset until offset + length step 2) {
            // expecting LINEAR16 in little endian.
            var s = payload[i + 1].toInt()
            if (s < 0) s *= -1
            s = s shl 8
            s += Math.abs(payload[i].toInt())
            if (s > 1500/*AMPLITUDE_THRESHOLD*/) {
                return true
            }
        }
        return false
    }

    override fun onWebSocketBinary(payload: ByteArray, offset: Int, length: Int) {
        logger.debug("onWebSocketBinary(payload[${payload.size}], offset = $offset, length = $length)")
        if (!inputAudioStreamCancelled) {
            if (isDetectingAudio(payload, offset, length))
                inputAudioTime = System.currentTimeMillis()
            if (inputAudioTime + 10000 < System.currentTimeMillis()) {
                val text = "#silence"
                inputAudioClose(true)
                sendEvent(BotEvent.Recognized(text))
                onInput(Input(clientRequirements.locale, clientRequirements.zoneId, Input.Transcript(text)))
            } else {
                super.onWebSocketBinary(payload, offset, length)
                sttStream?.write(payload, offset, length)
            }
        }
    }

    override fun onWebSocketText(json: String?) {
        try {
            val event = ObjectUtil.defaultMapper.readValue(json, BotEvent::class.java)
            logger.info("onWebSocketText(event = $event)")
            when (event) {
                // version 1 (deprecated - remove in version 3)
                is BotEvent.Requirements -> {
                    appKey = event.appKey
                    clientRequirements = event.requirements
                    sendEvent(event)
                }
                is BotEvent.Message -> onMessageEvent(event)
                // version 1+2
                is BotEvent.InputAudioStreamOpen -> {
                    inputAudioClose(false)
                    val sttConfig = SttConfig(clientRequirements.locale, clientRequirements.zoneId, clientRequirements.sttSampleRate)
                    sttService = SttServiceFactory.create("Google", sttConfig, this.expectedPhrases, BotSttCallback())
                    sttStream = sttService?.createStream()
                    inputAudioTime = System.currentTimeMillis()
                }
                is BotEvent.InputAudioStreamClose -> inputAudioClose(false)
                is BotEvent.InputAudioStreamCancel -> inputAudioClose(true)
                is BotEvent.SessionEnded -> sendEvent(BotEvent.SessionEnded())
                else -> error("Unexpected event of type ${event::class.simpleName}")
            }
        } catch (e: Exception) {
            sendEvent(BotEvent.Error(e.message?:e::class.qualifiedName?:"unknown"))
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


    fun onInput(input: Input) {
        val request = Request(appKey, sender, null, sessionId?:error("Session ID not set"), input)
        val response = coreResource.process(request)
        sendResponse(response)
        if (response.sessionEnded) {
            sendEvent(BotEvent.SessionEnded())
            inputAudioClose(false)
            sessionId = null
        }
    }

    // deprecated - remove in version 3
    private fun onMessageEvent(event: BotEvent.Message) = with (event) {
        onMessageEvent(event, Input(message.language?:Locale.ENGLISH, Defaults.zoneId, Input.Transcript(message.items.firstOrNull()?.text?:"")))
    }

    // deprecated - remove in version 3
    private fun onMessageEvent(event: BotEvent.Message, input: Input) = with (event) {

        val currentTime = System.currentTimeMillis()
        lastMessageTime = currentTime
        sender = message.sender
        language = message.language
        if (message.sessionId == null) {
            sessionId = Message.createId()
            sendEvent(BotEvent.SessionStarted(sessionId!!))
        } else {
            sessionId = message.sessionId
        }
        val request = Request(event.appKey, message.sender, null, sessionId?:error("Session ID not set"), input)
        coreResource.process(request).let {
            val response = message.response(it.items)
            response.sessionEnded = it.sessionEnded
            response.attributes["serviceResponseTime"] = System.currentTimeMillis() - currentTime
            expectedPhrases = response.expectedPhrases?: listOf()
            response.expectedPhrases = null
            if (response.sessionEnded) {
                sendResponse(response)
                sendEvent(BotEvent.SessionEnded())
                inputAudioClose(false)
                sessionId = null
            }
            // todo will not work correctly before the subdialogs in helena will be implemented
            else {
                sendResponse(response) // client will wait for user input
            }
        }
    }

    @Synchronized
    @Throws(IOException::class)
    override fun sendEvent(event: BotEvent) {
        logger.info("sendEvent(event = $event)")
        remote.sendString(ObjectUtil.defaultMapper.writeValueAsString(event))
    }

    override fun sendBinaryData(data: ByteArray, count: Int?) {
        logger.info("sendBinaryData(data[${data.size}])")
        remote.sendBytes(ByteBuffer.wrap(data))
    }

    @Throws(IOException::class)
    internal fun sendResponse(response: Response, ttsOnly: Boolean = false) {
        for (item in response.items) {
            if (item.text.isNullOrBlank()) {
                logger.debug("item.text.isNullOrBlank() == true")
                item.text = ""
            } else {
                val ttsRequest =
                        TtsRequest(
                                clientRequirements.ttsVoice?:item.ttsVoice?:TtsConfig.defaultVoice("en"),
                                ((if (item.ssml != null) item.ssml else item.text)?:"").replace(Regex("\\$(\\w+)")) {
                                    // command processing
                                    when (it.groupValues[1]) {
                                        "version" -> {
                                            item.text = "Server version ${AppConfig.version}, environment ${AppConfig.instance.get("namespace", "unknown")}."
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
                            item.audio = ServiceUrlResolver.getEndpointUrl("filestore", ServiceUrlResolver.RunMode.dist)  + '/' + audio.path // caller must know port URL therefore URI is enough

                        BotClientRequirements.TtsType.RequiredStreaming ->
                            sendBinaryData(audio.speak().data!!)
                    }
                }
            }
        }
        if (!ttsOnly) {
            if (lastMessageTime != null)
                (response.attributes as MutablePropertyMap)["portResponseTime"] = (System.currentTimeMillis() - lastMessageTime!!)
            sendEvent(
                BotEvent.Message(appKey,
                    if (response is Message)
                        response
                    else
                        Message(sender = "port", items = response.items, sessionEnded = response.sessionEnded)
                )
            )
        }
    }
}
