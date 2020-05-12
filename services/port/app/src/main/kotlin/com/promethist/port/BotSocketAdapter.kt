package com.promethist.port

import ai.promethist.client.BotClientRequirements
import ai.promethist.client.BotEvent
import ai.promethist.client.BotSocket
import com.promethist.common.AppConfig
import com.promethist.common.ObjectUtil
import com.promethist.common.ServiceUrlResolver
import com.promethist.core.ExpectedPhrase
import com.promethist.core.model.TtsConfig
import com.promethist.core.Input
import com.promethist.core.Request
import com.promethist.core.Response
import com.promethist.core.resources.CoreResource
import com.promethist.core.type.MutablePropertyMap
import com.promethist.port.stt.*
import com.promethist.port.tts.TtsRequest
import com.promethist.util.LoggerDelegate
import org.eclipse.jetty.websocket.api.WebSocketAdapter
import java.io.IOException
import java.nio.ByteBuffer
import java.util.*
import javax.inject.Inject

class BotSocketAdapter : BotSocket, WebSocketAdapter() {

    inner class BotSttCallback() : SttCallback {

        override fun onResponse(input: Input, final: Boolean) {
            try {
                if (final && !inputAudioStreamCancelled) {
                    sendEvent(BotEvent.Recognized(input.transcript.text))
                    onRequest(createRequest(input))
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

    private val logger by LoggerDelegate()

    private fun createRequest(input: Input) = Request(appKey, sender, sessionId?:error("missing session id"), input)

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
                val text = "#noaudio"
                inputAudioClose(true)
                sendEvent(BotEvent.Recognized(text))
                onRequest(createRequest(Input(clientRequirements.locale, clientRequirements.zoneId, Input.Transcript(text))))
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
                is BotEvent.Init -> {
                    appKey = event.key
                    sender = event.sender //TODO verify event.sender - get user id to be stored in connection
                    clientRequirements = event.requirements
                    sendEvent(BotEvent.Ready())
                }
                is BotEvent.Request -> onRequest(event.request)
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
        } catch (e: Throwable) {
            logger.error("onWebSocketText", e)
            (e.cause?:e).apply {
                sendEvent(BotEvent.Error(message?:this::class.qualifiedName?:"unknown"))
            }
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

    fun onRequest(request: Request) {
        if (sessionId == null) {
            sessionId = request.sessionId
            sendEvent(BotEvent.SessionStarted(request.sessionId))
        }
        val response = coreResource.process(request)
        if (response.expectedPhrases != null) {
            expectedPhrases = response.expectedPhrases!!
            response.expectedPhrases = null
        }
        sendResponse(response)
        if (response.sessionEnded) {
            sendEvent(BotEvent.SessionEnded())
            inputAudioClose(false)
            sessionId = null
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
        val items = response.items.toList()
        var shift = 0
        for (i in items.indices) {
            val item = items[i]
            if (item.text == null)
                item.text = ""
            var voice = clientRequirements.ttsVoice ?: item.ttsVoice ?: TtsConfig.defaultVoice("en")
            if (voice.startsWith('A')) {
                // Amazon Polly synthesis - strip <audio> tag and create audio item
                item.ssml = item.ssml?.replace(Regex("<audio.*?src=\"(.*?)\"[^\\>]+>")) {
                    if (item.text!!.isBlank())
                        item.audio = it.groupValues[1]
                    else
                        response.items.add(i + ++shift, Response.Item(audio = it.groupValues[1]))
                    ""
                }
            }
            // set voice by <voice> tag
            item.ssml = item.ssml?.replace(Regex("<voice.*?name=\"(.*?)\">(.*)</voice>")) {
                val name = it.groupValues[1]
                TtsConfig.values.forEach { config ->
                    if (name == config.name || name == config.voice)
                        voice = config.voice
                }
                it.groupValues[2]
            }
            if (item.audio == null && !item.text.isNullOrBlank()) {
                val ttsRequest =
                    TtsRequest(
                        voice,
                        ((if (item.ssml != null) item.ssml else item.text) ?: "").replace(Regex("#(\\w+)")) {
                            // command processing
                            when (it.groupValues[1]) {
                                "version" -> {
                                    item.text = "Server version ${AppConfig.version}, environment ${AppConfig.instance.get("namespace", "unknown")}."
                                    item.text!!
                                }
                                else -> ""
                            }
                        },
                        item.ssml != null
                    ).apply {
                        with(response) {
                            if (attributes.containsKey("speakingRate"))
                                speakingRate = attributes["speakingRate"].toString().toDouble()
                            if (attributes.containsKey("speakingPitch"))
                                speakingPitch = attributes["speakingPitch"].toString().toDouble()
                            if (attributes.containsKey("speakingVolumeGain"))
                                speakingVolumeGain = attributes["speakingVolumeGain"].toString().toDouble()
                        }
                    }
                if (clientRequirements.tts != BotClientRequirements.TtsType.None) {
                    val audio = dataService.getTtsAudio(
                            ttsRequest,
                            clientRequirements.tts != BotClientRequirements.TtsType.RequiredLinks,
                            clientRequirements.tts == BotClientRequirements.TtsType.RequiredStreaming
                    )
                    when (clientRequirements.tts) {
                        BotClientRequirements.TtsType.RequiredLinks ->
                            item.audio = ServiceUrlResolver.getEndpointUrl("filestore", ServiceUrlResolver.RunMode.dist) + '/' + audio.path

                        BotClientRequirements.TtsType.RequiredStreaming ->
                            sendBinaryData(audio.speak().data!!)
                    }
                }
            }
        }
        if (!ttsOnly) {
            if (lastMessageTime != null)
                (response.attributes as MutablePropertyMap)["portResponseTime"] = (System.currentTimeMillis() - lastMessageTime!!)
            sendEvent(BotEvent.Response(response))
        }
    }
}