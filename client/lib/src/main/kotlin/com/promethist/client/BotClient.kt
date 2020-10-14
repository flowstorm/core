package com.promethist.client

import com.promethist.client.audio.AudioCallback
import com.promethist.client.audio.AudioDevice
import com.promethist.client.audio.AudioRecorder
import com.promethist.core.Input
import com.promethist.core.Request
import com.promethist.core.Response
import com.promethist.core.model.SttConfig
import com.promethist.util.LoggerDelegate
import java.util.UUID

/**
 * Bot client
 */
class BotClient(
        val context: BotContext,
        val socket: BotSocket,
        val inputAudioDevice: AudioDevice? = null,
        val callback: BotClientCallback,
        val tts: BotConfig.TtsType = BotConfig.TtsType.RequiredLinks,
        val sttMode: SttConfig.Mode = SttConfig.Mode.Default,
        val pauseMode: Boolean = false,
        val inputAudioRecorder: AudioRecorder? = null
) : BotSocket.Listener {

    enum class State { Listening, Processing, Responding, Paused, Sleeping, Open, Closed, Failed }
    enum class Volume { _up, _down, _1, _2, _3, _4, _5, _6, _7, _8, _9, _10 }

    abstract class LazyThread(val delay: Int) : Thread() {
        var running = false

        override fun run() {
            running = true
            var counter = 0
            while (running && ++counter < delay)
                sleep(1000)
            running = false
            if (counter == delay)
                lazy()
        }

        abstract fun lazy()
    }

    var inputAudioStreamOpen = false
    val inputAudioQueue = InputAudioQueue(this).apply {
        Thread(this).start()
    }
    val outputQueue = OutputQueue(this).apply {
        Thread(this).start()
    }
    private var builtinAudioData = mutableMapOf<String, ByteArray?>()
    private var lastTime = System.currentTimeMillis()
    private var lastStateDuration = 0L
    private var waking = false
    var state = State.Closed
        set(state) {
            if (state != State.Sleeping)
                waking = false
            val currentTime = System.currentTimeMillis()
            lastStateDuration = currentTime - lastTime
            lastTime = currentTime
            logger.info("onStateChange(state = $state)")
            if ((state == State.Responding) && (sttMode == SttConfig.Mode.Duplex) && !inputAudioStreamOpen) {
                inputAudioStreamOpen()
            } else {
                callback.onBotStateChange(this, state)
                field = state
            }
        }
    private val logger by LoggerDelegate()
    private var lostThread: LazyThread? = null

    fun open() {
        logger.info("open()")
        if (inputAudioDevice != null) {
            inputAudioDevice.callback = object : AudioCallback {
                override fun onStart() = logger.info("Audio input start")
                override fun onStop() = logger.info("Audio input stop")
                override fun onData(buf: ByteArray, count: Int) = buf.copyOf(count).let {
                    inputAudioRecorder?.write(it)
                    if (inputAudioStreamOpen) inputAudioQueue.add(it) else true
                }

                override fun onWake() {
                    callback.onWakeWord(this@BotClient)
                    touch()
                }
            }
            inputAudioDevice.start()
            if (inputAudioDevice is Runnable)
                Thread(inputAudioDevice).start()
        }
        socket.listener = this
        socket.open()
        callback.onOpen(this)
    }

    override fun onOpen() {
        logger.info("onOpen()")
        state = State.Open
        val config = BotConfig(tts = tts, voice = context.voice, sttMode = sttMode)
        socket.sendEvent(BotEvent.Init(context.key, context.sender, context.token, config))
    }

    fun close() {
        logger.info("close()")
        inputAudioRecorder?.stop()
        inputAudioStreamClose(false)
        outputAudioPlayCancel()
        socket.close()
        inputAudioDevice?.close()
        outputQueue.close()
        callback.onClose(this)
    }

    override fun onClose() {
        logger.info("onClose()")
        state = State.Closed
    }

    private fun outputAudioPlayCancel() {
        outputQueue.clear(OutputQueue.Item.Type.Server)
        callback.audioCancel()
    }

    override fun onEvent(event: BotEvent) {
        logger.debug("onBotEvent(event = $event)")
        when (event) {
            is BotEvent.Ready -> onReady()
            is BotEvent.SessionStarted -> onSessionStarted(event.sessionId)
            is BotEvent.SessionEnded -> onSessionEnded()
            is BotEvent.InputAudioStreamOpen -> onInputAudioStreamOpen()
            is BotEvent.Recognized -> onRecognized(event.text)
            is BotEvent.Response -> onResponse(event.response)
            is BotEvent.Error -> onError(event.text)
            else -> logger.warn("unknown event type ${event::class.simpleName}")
        }
    }

    fun onReady() {
        callback.onReady(this)
        state = State.Sleeping
        if (context.autoStart) {
            doIntro()
        } else {
            if (lostThread?.running == true)
                lostThread!!.running = false
            else
                builtinAudio("${context.locale.language}/bot_ready")
        }
    }

    private fun onError(text: String) {
        logger.error("onError(text = $text)")
        inputAudioRecorder?.stop()
        context.sessionId = null
        inputAudioStreamClose(false)
        builtinAudio("${context.locale.language}/error")
        callback.onError(this, text)
        state = State.Sleeping
    }

    private fun onSessionStarted(sessionId: String) {
        logger.info("onSessionStarted(sessionId = $sessionId)")
        context.sessionId = sessionId
        callback.onSessionId(this, context.sessionId)
        inputAudioRecorder?.start(sessionId)
        state = State.Sleeping
    }

    private fun onSessionEnded() {
        logger.info("onSessionEnded()")
        inputAudioStreamClose(false)
        builtinAudio("sleep")
        context.sessionId = null
        callback.onSessionId(this, context.sessionId)
        state = State.Sleeping
    }

    private fun onResponse(response: Response) {
        outputAudioPlayCancel()
        if (response.logs.isNotEmpty())
            callback.onLog(this, response.logs)

        if (state != State.Responding)
            state = State.Responding
        outputQueue.suspended = true
        try {
            for (item in response.items) {
                val text = (item.text ?: "").replace(Regex("\\$(\\w+)")) {
                    val command = it.groupValues[1]
                    if (command.startsWith("volume_"))
                        callback.onVolumeChange(this, Volume.valueOf(command.substring(6)))
                    ""
                }.trim()
                if (text.isNotEmpty()) {
                    outputQueue.add(OutputQueue.Item.Text(text))
                }
                if (item.audio != null) {
                    val url = (if (item.audio!!.startsWith('/')) context.url else "") + item.audio
                    val data = callback.httpRequest(this, url) ?: error("missing audio data from url $url")
                    outputQueue.add(OutputQueue.Item.Audio(data))
                }
                if (item.image != null) {
                    val url = (if (item.image!!.startsWith('/')) context.url else "") + item.image
                    outputQueue.add(OutputQueue.Item.Image(url))
                }
            }
        } finally {
            outputQueue.suspended = false
        }
    }

    private fun onRecognized(text: String) {
        logger.info("onRecognized(text = $text)")
        callback.onRecognized(this, text)
        outputAudioPlayCancel()
        builtinAudio("recognized")
        builtinAudio("waiting", OutputQueue.Item.Type.Server)
        state = State.Processing
        if (sttMode != SttConfig.Mode.Duplex)
           inputAudioStreamClose()
    }

    fun touch(openInputAudio: Boolean = true) {
        logger.info("touch(openInputAudio = $openInputAudio), state = $state")
        when (state) {
            State.Closed, State.Failed -> {
                if (lostThread != null && lostThread!!.running)
                    lostThread!!.running = false
                builtinAudio("${context.locale.language}/connection_lost")
            }
            State.Open -> {
                outputAudioPlayCancel()
                if (openInputAudio)
                    inputAudioStreamOpen()
            }
            State.Responding -> {
                if (pauseMode) {
                    if (inputAudioDevice != null)
                        state = State.Paused
                } else {
                    outputAudioPlayCancel()
                    if (sttMode != SttConfig.Mode.Duplex)
                        inputAudioStreamOpen()
                }
            }
            State.Paused -> {
                state = State.Responding
            }
            State.Sleeping -> {
                outputAudioPlayCancel()
                waking = true
                doIntro()
            }
        }
    }

    fun audio(data: ByteArray) {
        logger.info("audio(data = ByteArray[${data.size}])")
        callback.audio(this, data)
    }

    fun image(url: String) {
        logger.info("image(url = $url)")
        callback.image(this, url)
    }

    fun text(text: String) {
        logger.info("text(text = $text)")
        callback.text(this, text)
    }

    override fun onAudioData(data: ByteArray) {
        outputQueue.add(OutputQueue.Item.Audio(data))
    }

    override fun onFailure(t: Throwable) {
        if (socket.state == BotSocket.State.Failed && state != State.Failed) {
            inputAudioStreamClose(false)
            if (lostThread == null || !lostThread!!.running)
                lostThread = object : LazyThread(20) {
                    override fun lazy() {
                        builtinAudio("${context.locale.language}/connection_lost")
                    }
                }.apply { start() }
            callback.onFailure(this, t)
        }
        state = State.Failed
    }

    private fun builtinAudio(name: String, type: OutputQueue.Item.Type = OutputQueue.Item.Type.Client) =
            outputQueue.add(OutputQueue.Item.Audio(builtinAudioData.getOrPut(name) {
                if (name.endsWith("/bot_ready") || name.endsWith("/connection_lost") || name.endsWith("/error"))
                    javaClass.getResourceAsStream("/audio/$name.mp3").readBytes()
                else
                    callback.httpRequest(this, "https://repository.promethist.ai/audio/client/$name.mp3")
            } ?: error("missing builtin audio $name"), type))

    fun inputAudioStreamOpen() {
        if (inputAudioDevice != null) {
            logger.info("inputAudioStreamOpen()")
            if (!inputAudioStreamOpen) {
                builtinAudio("listening")
                state = State.Listening
                inputAudioQueue.clear()
                inputAudioDevice.start()
                socket.sendEvent(BotEvent.InputAudioStreamOpen())
            }
        }
    }

    private fun onInputAudioStreamOpen() {
        logger.info("onInputAudioStreamOpen()")
        inputAudioStreamOpen = true
    }

    private fun inputAudioStreamClose(sendEvent: Boolean = true) {
        if (inputAudioDevice != null) {
            logger.info("inputAudioStreamClose()")
            if (inputAudioStreamOpen && sendEvent)
                socket.sendEvent(BotEvent.InputAudioStreamClose())
            inputAudioStreamOpen = false
        }
    }

    fun sendInputAudioData(data: ByteArray, count: Int) {
        logger.debug("sendInputAudioData($count bytes)")
        socket.sendAudioData(data, count)
    }

    fun doText(text: String) {
        if (context.sessionId == null)
            context.sessionId = UUID.randomUUID().toString()
        val request = Request(context.key, context.sender, context.token, context.sessionId!!, context.initiationId, Input(context.locale, context.zoneId, Input.Transcript(text)), context.attributes)
        socket.sendEvent(BotEvent.Request(request))
    }

    private fun doIntro() {
        builtinAudio("intro")
        doText(context.introText)
    }

}