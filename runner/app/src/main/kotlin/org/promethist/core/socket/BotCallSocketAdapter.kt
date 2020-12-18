package org.promethist.core.socket

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.promethist.client.BotConfig
import org.promethist.client.BotEvent
import org.promethist.common.AppConfig
import org.promethist.common.ObjectUtil.defaultMapper
import org.promethist.core.Defaults
import org.promethist.core.Input
import org.promethist.core.model.SttConfig
import org.promethist.core.type.Dynamic
import org.promethist.util.DataConverter
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.*

class BotCallSocketAdapter : AbstractBotSocketAdapter() {

    data class Mark(val name: String)

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "event")
    @JsonSubTypes(
            JsonSubTypes.Type(value = InputMessage.Connected::class, name = "connected"),
            JsonSubTypes.Type(value = InputMessage.Start::class, name = "start"),
            JsonSubTypes.Type(value = InputMessage.Stop::class, name = "stop"),
            JsonSubTypes.Type(value = InputMessage.Mark::class, name = "mark"),
            JsonSubTypes.Type(value = InputMessage.Media::class, name = "media")
    )
    open class InputMessage {
        data class Connected(val protocol: String, val version: String) : InputMessage()
        data class Start(val sequenceNumber: String, val start: Start, val streamSid: String) : InputMessage() {
            data class MediaFormat(val encoding: String, val sampleRate: Int, val channels: Int)
            data class Start(val accountSid: String, val streamSid: String, val callSid: String, val tracks: List<String>, val customParameters: Map<String, String>, val mediaFormat: MediaFormat)
        }
        data class Stop(val sequenceNumber: String) : InputMessage()
        data class Mark(val sequenceNumber: String, val mark: BotCallSocketAdapter.Mark) : InputMessage()
        data class Media(val sequenceNumber: String, val media: Media, val streamSid: String) : InputMessage() {
            data class Media(val track: String, val chunk: String, val timestamp: String, val payload: String) {
                val payloadBytes: ByteArray get() = Base64.getDecoder().decode(payload)
            }
        }
    }

    open class OutputMessage(val event: String) {
        data class Media(val streamSid: String, val media: Media) : OutputMessage("media") {
            class Media(bytes: ByteArray) {
                val payload = Base64.getEncoder().encodeToString(bytes)
            }
        }
        data class Clear(val streamSid: String) : OutputMessage("clear")
        data class Mark(val streamSid: String, val mark: BotCallSocketAdapter.Mark) : OutputMessage("mark")
    }

    override lateinit var appKey: String
    override lateinit var deviceId: String
    override var token: String? = null
    override var config = BotConfig(Defaults.locale, Defaults.zoneId, true, SttConfig.Mode.Duplex, 8000, BotConfig.TtsType.RequiredStreaming)
    override val sttConfig
        get() = SttConfig(locale
                ?: config.locale, config.zoneId, config.sttSampleRate, SttConfig.Encoding.MULAW, config.sttMode)
    private var streamSid: String? = null
    private val workDir = File(System.getProperty("java.io.tmpdir"))
    private val outSound = javaClass.getResourceAsStream("/audio/out.mp3").readBytes()

    private fun sendMessage(message: OutputMessage) = remote.sendString(defaultMapper.writeValueAsString(message))

    override fun sendEvent(event: BotEvent) {
        logger.info("call event $event")
        when (event) {
            is BotEvent.SessionEnded -> {
                val mark = OutputMessage.Mark(streamSid!!, Mark("Sleeping"))
                sendMessage(mark)
            }
            is BotEvent.Recognized -> {
                sendMessage(OutputMessage.Clear(streamSid!!))
                sendAudioData(outSound)
                if (!inputAudioStreamOpen/* && sttConfig.mode == SttConfig.Mode.Duplex*/)
                    inputAudioStreamOpen()
            }
        }
    }

    override fun sendAudioData(data: ByteArray, count: Int?) {
        val payload = getMulawData(data)
        val message = OutputMessage.Media(streamSid!!, OutputMessage.Media.Media(payload))
        logger.info("call media ${data.size} (MP3) > ${payload.size} (MULAW) bytes")
        sendMessage(message)
    }

    override fun onWebSocketText(json: String?) {
        when (val message = defaultMapper.readValue(json, InputMessage::class.java)) {
            is InputMessage.Start -> {
                streamSid = message.streamSid
                sessionId = message.start.callSid
                message.start.customParameters.let {
                    deviceId = it["deviceId"] ?: it["sender"] ?: "anonymous"
                    appKey = it["appKey"] ?: "promethist"
                    val initiationId: String? = it["initiationId"]
                    if (it.containsKey("locale"))
                        config.locale = Locale.forLanguageTag(it["locale"])
                    //TODO zoneId from zip/city/state/country

                    logger.info("call from $deviceId")
                    onRequest(
                        createRequest(
                            Input(transcript = Input.Transcript("#intro")),
                            initiationId,
                            Dynamic("clientType" to "call:" + AppConfig.instance.get("git.ref", "unknown"))
                        )
                    )
                    inputAudioStreamOpen()
                }
            }
            is InputMessage.Stop -> {
                session.close()
            }
            is InputMessage.Mark -> {
                if (message.mark.name == "Sleeping")
                    session.close()
            }
            is InputMessage.Media -> {
                if (inputAudioStreamOpen) {
                    val payload = message.media.payloadBytes
                    onInputAudio(payload, 0, payload.size)
                }
            }
        }
    }

    private fun getMulawData(data: ByteArray): ByteArray {
        val code = DataConverter.digest(data)
        val mulawFile = File(workDir, "$code.mulaw")
        if (!mulawFile.exists()) {
            logger.info("generating MULAW $code")
            val mp3File = File(workDir, "$code.mp3")
            mp3File.writeBytes(data)
            ProcessBuilder(
                    "/usr/local/bin/ffmpeg", "-y", "-i", mp3File.absolutePath,
                    "-codec:a", "pcm_mulaw", "-ar", "8k", "-ac", "1",
                    "-f", "mulaw", mulawFile.absolutePath
            ).apply {
                redirectErrorStream(true)
                val buf = StringBuilder()
                val proc = start()
                val input = BufferedReader(InputStreamReader(proc.inputStream))
                while (true)
                    buf.appendln(input.readLine() ?: break)
                if (proc.waitFor() != 0)
                    error(buf)
            }
        }
        return mulawFile.readBytes()
    }
}