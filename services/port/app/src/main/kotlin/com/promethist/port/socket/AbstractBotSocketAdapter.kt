package com.promethist.port.socket

import com.promethist.client.BotConfig
import com.promethist.client.BotEvent
import com.promethist.client.BotSocket
import com.promethist.common.AppConfig
import com.promethist.common.ServiceUrlResolver
import com.promethist.core.ExpectedPhrase
import com.promethist.core.Input
import com.promethist.core.Request
import com.promethist.core.Response
import com.promethist.core.model.TtsConfig
import com.promethist.core.model.Voice
import com.promethist.core.resources.CoreResource
import com.promethist.core.type.Dynamic
import com.promethist.core.type.MutablePropertyMap
import com.promethist.port.PortService
import com.promethist.port.stt.*
import com.promethist.port.tts.TtsRequest
import com.promethist.util.LoggerDelegate
import org.eclipse.jetty.websocket.api.WebSocketAdapter
import java.io.IOException
import java.util.*
import javax.inject.Inject
import kotlin.concurrent.thread

abstract class AbstractBotSocketAdapter : BotSocket, WebSocketAdapter() {

    inner class BotSttCallback : SttCallback {

        var silence = true

        override fun onResponse(input: Input, isFinal: Boolean) {
            try {
                if (isFinal && !isRecognitionCancelled) {
                    silence = false
                    stopRecognition()
                    sendEvent(BotEvent.Recognized(input.transcript.text))
                    onRequest(createRequest(input))
                }
            } catch (e: IOException) {
                onError(e)
            }
        }

        override fun onError(e: Throwable) {
            logger.error("STT callback error", e)
            if (isConnected)
                sendEvent(BotEvent.Error(e.message ?: ""))
        }

        override fun onOpen() {
            sendEvent(BotEvent.InputAudioStreamOpen())
        }

        override fun onEndOfUtterance() {
            silence = true
            stopRecognition()
            thread {
                Thread.sleep(1500)
                if (silence) {
                    sendSilenceResponse()
                }
            }
        }
    }

    @Inject
    lateinit var coreResource: CoreResource

    @Inject
    lateinit var dataService: PortService

    protected val logger by LoggerDelegate()
    abstract var config: BotConfig
    override var state = BotSocket.State.Open
    override var listener: BotSocket.Listener? = null
    abstract var appKey: String
    abstract var sender: String
    abstract var token: String?
    protected var locale: Locale? = null
    protected var sessionId: String? = null
    private var expectedPhrases: List<ExpectedPhrase> = listOf()
    private var recognitionStartTime: Long = 0
    private val sttService: SttService = SttServiceFactory.create("Google", BotSttCallback())
    private var sttStream: SttStream? = null
    protected var isRecognitionCancelled = false
    protected val isRecognitionStarted get() = (sttStream != null)

    fun createRequest(input: Input, attributes: MutablePropertyMap = Dynamic()) =
            Request(appKey, sender, token, sessionId ?: error("missing session id"), input, attributes)

    override fun open() = logger.info("open()")

    override fun close() {
        logger.info("close()")
        if (isRecognitionStarted)
            stopRecognition()
        sttService.close()
    }

    fun startRecognition(sttConfig: SttConfig) {
        logger.info("startRecognition(sttConfig = $sttConfig)")
        sttStream = sttService.createStream(sttConfig, expectedPhrases)
        recognitionStartTime = System.currentTimeMillis()
    }

    fun stopRecognition(cancelled: Boolean = false) {
        logger.info("stopRecognition(cancelled = $cancelled)")
        isRecognitionCancelled = cancelled
        sttStream?.close()
        sttStream = null
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

    fun onInputAudio(payload: ByteArray, offset: Int, length: Int) {
        logger.debug("onInputAudio(payload[${payload.size}], offset = $offset, length = $length)")
        if (isRecognitionStarted) {
            if (isDetectingAudio(payload, offset, length))
                recognitionStartTime = System.currentTimeMillis()
            if (recognitionStartTime + 10000 < System.currentTimeMillis()) {
                stopRecognition(true)
                sendSilenceResponse()
            } else {
                sttStream?.write(payload, offset, length)
            }
        }
    }

    private fun sendSilenceResponse() {
        sendEvent(BotEvent.Recognized(ACTION_SILENCE))
        onRequest(createRequest(Input(config.locale, config.zoneId, Input.Transcript(ACTION_SILENCE))))
    }

    override fun onWebSocketClose(statusCode: Int, reason: String?) {
        super.onWebSocketClose(statusCode, reason)
        close()
    }

    override fun onWebSocketError(cause: Throwable?) {
        super.onWebSocketError(cause)
        close()
    }

    open fun onRequest(request: Request) {
        if (sessionId == null) {
            sessionId = request.sessionId
            sendEvent(BotEvent.SessionStarted(request.sessionId))
        }
        val response = coreResource.process(request)
        if (response.expectedPhrases != null) {
            expectedPhrases = response.expectedPhrases!!
            response.expectedPhrases = null
        }
        locale = response.locale
        sendResponse(response)
        if (response.sessionEnded) {
            sendEvent(BotEvent.SessionEnded())
            stopRecognition()
            sessionId = null
        }
    }

    @Throws(IOException::class)
    internal fun sendResponse(response: Response, ttsOnly: Boolean = false) {
        val items = response.items.toList()
        var shift = 0
        for (i in items.indices) {
            val item = items[i]
            if (item.text == null)
                item.text = ""
            var voice = config.voice ?: item.voice ?: TtsConfig.defaultVoice(response.locale?.language ?: "en")
            if (voice.name.startsWith('A')) {
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
                    if (name == config.name || name == config.voice.name)
                        voice = config.voice
                }
                it.groupValues[2]
            }

            // set style by <voice> tag
            var ttsStyle = ""
            item.ssml = item.ssml?.replace(Regex("<voice.*?style=\"(.*?)\">(.*)</voice>")) {
                ttsStyle = it.groupValues[1]
                it.groupValues[2]
            }

            if (item.audio == null && !item.text.isNullOrBlank()) {
                val ttsRequest =
                        TtsRequest(
                                voice,
                                ((if (item.ssml != null) item.ssml else item.text) ?: "").replace(Regex("#(\\w+)")) {
                                    // command processing
                                    when (it.groupValues[1]) {
                                        //TO BE REMOVED version command is now handled by a global intent in Basic Dialogue classes for each specific localization
                                        "version" -> {
                                            item.text = "Server version ${AppConfig.version}, environment ${AppConfig.instance.get("namespace", "unknown")}."
                                            item.voice = Voice.Audrey
                                            item.text!!
                                        }
                                        else -> ""
                                    }
                                },
                                item.ssml != null,
                                ttsStyle
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
                if (config.tts != BotConfig.TtsType.None) {
                    val audio = dataService.getTtsAudio(
                            ttsRequest,
                            config.tts != BotConfig.TtsType.RequiredLinks,
                            config.tts == BotConfig.TtsType.RequiredStreaming
                    )
                    when (config.tts) {
                        BotConfig.TtsType.RequiredLinks ->
                            item.audio = ServiceUrlResolver.getEndpointUrl("filestore", ServiceUrlResolver.RunMode.dist) + '/' + audio.path

                        BotConfig.TtsType.RequiredStreaming ->
                            sendAudioData(audio.speak().data!!)
                    }
                }
            }
        }
        if (!ttsOnly)
            sendEvent(BotEvent.Response(response))
    }

    companion object {
        const val ACTION_SILENCE = "#silence"
    }
}