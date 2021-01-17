package org.promethist.core.socket

import com.google.api.gax.rpc.OutOfRangeException
import org.eclipse.jetty.websocket.api.WebSocketAdapter
import org.promethist.client.BotConfig
import org.promethist.client.BotEvent
import org.promethist.client.BotSocket
import org.promethist.common.AppConfig
import org.promethist.common.ServiceUrlResolver
import org.promethist.common.monitoring.Monitor
import org.promethist.core.*
import org.promethist.core.model.SttConfig
import org.promethist.core.model.Voice
import org.promethist.core.storage.FileStorage
import org.promethist.core.stt.SttCallback
import org.promethist.core.stt.SttService
import org.promethist.core.stt.SttServiceFactory
import org.promethist.core.stt.SttStream
import org.promethist.core.tts.TtsAudioService
import org.promethist.core.tts.TtsRequest
import org.promethist.core.type.Dynamic
import org.promethist.core.type.MutablePropertyMap
import org.promethist.util.LoggerDelegate
import java.io.IOException
import java.util.*
import javax.inject.Inject
import kotlin.concurrent.thread

abstract class AbstractBotSocketAdapter : BotSocket, WebSocketAdapter() {

    @Inject
    lateinit var fileStorage: FileStorage

    @Inject
    lateinit var ttsAudioService: TtsAudioService

    @Inject
    lateinit var monitor: Monitor

    inner class BotSttCallback : SttCallback {

        var silence = true

        override fun onResponse(input: Input, isFinal: Boolean) {
            try {
                if (isFinal && inputAudioStreamOpen) {
                    silence = false
                    inputAudioStreamClose()
                    sendEvent(BotEvent.Recognized(input.transcript.text))
                    onRequest(createRequest(input))
                } else {
                    sttLastTime = System.currentTimeMillis()
                }
            } catch (e: IOException) {
                onError(e)
            }
        }

        override fun onError(e: Throwable) {
            monitor.capture(e, mapOf(
                    "config" to config,
                    "sessionId" to sessionId,
                    "appKey" to appKey
            ))
            logger.error("BotSttCallback.onError", e)
            if (isConnected) {
                if (e is OutOfRangeException)
                    onSilence()
                else
                    sendEvent(BotEvent.Error(e.message ?: ""))
            }
        }

        override fun onOpen() {
            sendEvent(BotEvent.InputAudioStreamOpen())
        }

        override fun onEndOfUtterance() {
            silence = true
            thread {
                Thread.sleep(1500)
                if (silence)
                    onSilence()
            }
        }
    }

    protected val logger by LoggerDelegate()
    abstract var config: BotConfig
    override var state = BotSocket.State.Open
    override var listener: BotSocket.Listener? = null
    abstract var appKey: String
    abstract var deviceId: String
    abstract var token: String?
    protected var locale: Locale? = null
    protected var sessionId: String? = null
    private var expectedPhrases: List<ExpectedPhrase> = listOf()
    private var sttLastTime: Long = 0
    private val sttService: SttService = SttServiceFactory.create("Google", BotSttCallback())
    private var sttStream: SttStream? = null
    abstract val sttConfig: SttConfig
    protected val inputAudioStreamOpen get() = (sttStream != null)
    protected val attributes = mutableMapOf<String, Any>()

    fun createRequest(input: Input, initiationId: String? = null, attributes: MutablePropertyMap = Dynamic()) =
            Request(appKey, deviceId, token, sessionId ?: error("missing session id"), initiationId, input, attributes)

    override fun open() = logger.info("Open")

    override fun close() {
        logger.info("Close")
        if (inputAudioStreamOpen)
            inputAudioStreamClose(true)
        sttService.close()
    }

    fun inputAudioStreamOpen() {
        if (sttStream == null) {
            logger.info("Opening STT stream")
            sttStream = sttService.createStream(sttConfig, expectedPhrases)
        }
        sttLastTime = System.currentTimeMillis()
    }

    fun inputAudioStreamClose(sttClose: Boolean = (sttConfig.mode != SttConfig.Mode.Duplex)) {
        if (sttClose && inputAudioStreamOpen) {
            logger.info("Closing STT stream")
            sttStream?.close()
            sttStream = null
        }
    }

    fun onInputAudio(payload: ByteArray, offset: Int, length: Int) {
        logger.debug("Input audio ${payload.size} received (offset=$offset, length=$length)")
        if (inputAudioStreamOpen) {
            if ((sttLastTime + config.silenceTimeout < System.currentTimeMillis()) && (sttConfig.mode != SttConfig.Mode.Duplex))
                onSilence()
            else
                sttStream?.write(payload, offset, length)
        }
    }

    private fun onSilence() {
        inputAudioStreamClose()
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
        attributes.putAll(request.attributes)
        request.attributes = attributes
        val response = BotCore.resource.process(request)
        attributes.clear()
        if (response.expectedPhrases != null) {
            expectedPhrases = response.expectedPhrases!!
            response.expectedPhrases = null
        }
        if (response.sttMode != null) {
            sttConfig.mode = response.sttMode!!
        }
        locale = response.locale
        sendResponse(response)
        if (response.sessionEnded) {
            sendEvent(BotEvent.SessionEnded())
            inputAudioStreamClose(true)
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
            var ttsConfig = item.ttsConfig ?: config.ttsConfig ?: Voice.forLanguage(response.locale?.language ?: "en").config
            if (ttsConfig.provider.startsWith('A')) {
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
                Voice.values().find { voice -> name == voice.name || name == voice.config.name } ?.let { voice ->
                    ttsConfig = voice.config
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
                        ttsConfig,
                        ((if (item.ssml != null) item.ssml else item.text) ?: "").replace(Regex("#(\\w+)")) {
                            // command processing
                            when (it.groupValues[1]) {
                                //TO BE REMOVED version command is now handled by a global intent in Basic Dialogue classes for each specific localization
                                "version" -> {
                                    item.text = "Server version ${AppConfig.version}, environment ${AppConfig.instance.get("namespace", "unknown")}."
                                    item.ttsConfig = Voice.Audrey.config
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
                if (config.tts != BotConfig.TtsType.None && ttsRequest.text.isNotBlank()) {
                    val audio = ttsAudioService.get(
                            ttsRequest,
                            config.tts != BotConfig.TtsType.RequiredLinks,
                            config.tts == BotConfig.TtsType.RequiredStreaming
                    )
                    when (config.tts) {
                        BotConfig.TtsType.RequiredLinks ->
                            item.audio = ServiceUrlResolver.getEndpointUrl("core") + "/file/" + audio.path

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