package ai.flowstorm.client.standalone.cli

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.type.TypeReference
import com.pi4j.io.gpio.GpioFactory
import com.pi4j.io.gpio.PinPullResistance
import com.pi4j.io.gpio.PinState
import com.pi4j.io.gpio.RaspiPin
import com.pi4j.io.gpio.event.GpioPinListenerDigital
import cz.alry.jcommander.CommandRunner
import ai.flowstorm.client.BotClient
import ai.flowstorm.client.BotConfig
import ai.flowstorm.client.BotContext
import ai.flowstorm.client.audio.SpeechDevice
import ai.flowstorm.client.audio.WavFileAudioRecorder
import ai.flowstorm.client.common.JwsBotClientSocket
import ai.flowstorm.client.common.OkHttp3BotClientSocket
import ai.flowstorm.client.signal.SignalGroup
import ai.flowstorm.client.signal.SignalProvider
import ai.flowstorm.client.standalone.Application
import ai.flowstorm.client.standalone.DeviceClientCallback
import ai.flowstorm.client.standalone.io.*
import ai.flowstorm.client.standalone.ui.Screen
import ai.flowstorm.client.util.HttpUtil
import ai.flowstorm.client.util.InetInterface
import ai.flowstorm.common.AppConfig
import ai.flowstorm.common.ObjectUtil.defaultMapper
import ai.flowstorm.common.ServiceUrlResolver
import ai.flowstorm.core.model.SttConfig
import ai.flowstorm.core.model.Voice
import ai.flowstorm.core.AudioFileType
import ai.flowstorm.core.type.Dynamic
import ai.flowstorm.core.type.PropertyMap
import org.slf4j.LoggerFactory
import java.awt.Color
import java.io.*
import java.util.*
import kotlin.concurrent.thread
import kotlin.system.exitProcess

class ClientCommand: CommandRunner<Application.Config, ClientCommand.Config> {

    enum class BotSocketType { OkHttp3, JWS }

    @Parameters(commandNames = ["client"], commandDescription = "Run client (press Ctrl+C to quit)")
    class Config : ClientConfig() {

        var version = 1

        @Parameter(names = ["-c", "--config"], order = 0, description = "Configuration file")
        var fileConfig: String? = null

        @Parameter(names = ["-sc", "--serverConfig"], order = 0, description = "Allow server configuration")
        var serverConfig = false

        @Parameter(names = ["-d", "--device"], order = 1, description = "Device type (e.g. desktop, rpi)")
        var device = "desktop"

        @Parameter(names = ["-e", "--environment"], order = 2, description = "Environment (develop, preview) - this superseeds -u value")
        var environment: String? = null

        @Parameter(names = ["-nc", "--noCache"], order = 3, description = "Do not cache anything")
        var noCache = false

        @Parameter(names = ["-id", "-s", "--sender"], order = 4, description = "Device identification")
        var deviceId = "standalone_" + (InetInterface.getActive()?.hardwareAddress?.replace(":", "") ?: "default")

        @Parameter(names = ["-it", "--introText"], order = 5, description = "Intro text")
        var introText: String? = null

        @Parameter(names = ["-as", "--autoStart"], order = 6, description = "Start conversation automatically")
        var autoStart = false

        @Parameter(names = ["-ex", "--exitOnError"], order = 7, description = "Raise exceptions")
        var exitOnError = false

        @Parameter(names = ["-nol", "--noOutputLogs"], order = 8, description = "No output logs")
        var noOutputLogs = false

        @Parameter(names = ["-log", "--showLogs"], order = 9, description = "Show contextual logs")
        var showLogs = false

        // audio

        @Parameter(names = ["-stt", "--sttMode"], order = 29, description = "STT mode (Default, SingleUtterance, Duplex)")
        var sttMode = SttConfig.Mode.SingleUtterance

        @Parameter(names = ["-tts", "--ttsFileType"], order = 30, description = "TTS file type (mp3, wav)")
        var ttsFileType = AudioFileType.mp3

        @Parameter(names = ["-v", "--voice"], order = 31, description = "TTS voice")
        var voice: Voice? = null

        @Parameter(names = ["-pn", "--portName"], order = 32, description = "Audio output port name")
        var portName: String = "SPEAKER"

        @Parameter(names = ["-vo", "--volume"], order = 33, description = "Audio output volume")
        var volume: Int? = null

        @Parameter(names = ["-nia", "--noInputAudio"], order = 34, description = "No input audio (text input only)")
        var noInputAudio = false

        @Parameter(names = ["-noa", "--noOutputAudio"], order = 35, description = "No output audio (text output only)")
        var noOutputAudio = false

        @Parameter(names = ["-aru", "--audioRecordUpload"], order = 36, description = "Audio record with upload (none, local, night, immediate)")
        var audioRecordUpload = WavFileAudioRecorder.UploadMode.none

        @Parameter(names = ["-pm", "--pauseMode"], order = 37, description = "Pause mode (wake word or button will pause output audio instead of stopping it and listening)")
        var pauseMode = false

        // GUI

        @Parameter(names = ["-scr", "--screen"], order = 40, description = "Screen view (none, window, fullscreen)")
        var screen = "none"

        @Parameter(names = ["-nan", "--noAnimations"], order = 41, description = "No animations")
        var noAnimations = false

        // networking

        @Parameter(names = ["-sp", "--socketPing"], order = 80, description = "Socket ping period (in seconds, 0 = do not ping)")
        var socketPing = 10L

        @Parameter(names = ["-st", "--socketType"], order = 81, description = "Socket implementation type (OkHttp3, JWS)")
        var socketType = BotSocketType.OkHttp3

        @Parameter(names = ["-aa", "--autoUpdate"], order = 82, description = "Auto update JAR file")
        var autoUpdate = false

        @Parameter(names = ["-du", "--distUrl"], order = 83, description = "Distribution URL for auto updates")
        var distUrl = "https://repository.promethist.ai/dist"
    }

    lateinit var out: PrintWriter
    lateinit var config: Config
    lateinit var context: BotContext
    lateinit var client: BotClient
    lateinit var callback: DeviceClientCallback
    lateinit var speechDevice: SpeechDevice
    var light: Light? = null
    var responded = false

    private fun exit() {
        client.close()
        Thread.sleep(2000)
        exitProcess(0)
    }

    private fun setOutput() {
        out = PrintWriter(
                OutputStreamWriter(
                if (config.output == "stdout")
                    System.out
                else
                    FileOutputStream(config.output)
        ), true)
    }

    private fun setVolume() {
        if (config.volume != null) {
            OutputAudioDevice.volume(config.portName, config.volume!!)
            out.println("{Volume ${config.portName} set to ${config.volume}}\n")
        }
    }

    private fun loadConfig(input: InputStream) = input.use {
        defaultMapper.readerForUpdating(config).readValue(JsonFactory().createParser(it), object : TypeReference<Config>() {})
    }

    private fun loadConfig() {
        if (config.fileConfig != null) {
            println("{Configuration from ${config.fileConfig}}")
            loadConfig(FileInputStream(config.fileConfig))
        } else if (config.serverConfig) {
            val url = Application.getServiceUrl("admin", config.environment ?: "production") + "/client/deviceConfig/${config.deviceId}"
            try {
                HttpUtil.httpRequestStream(url, raiseExceptions = true)?.let {
                    loadConfig(it)
                    out.println("{Configuration from $url}")
                }
            } catch (e: Throwable) {
                out.println("{Configuration from $url error - ${e.message}}")
            }
        }
    }

    private fun createContext() {
        ServiceUrlResolver
        context = BotContext(
                url = if (config.environment != null)
                    Application.getServiceUrl("core", config.environment ?: "production")
                else
                    config.url,
                key = config.key,
                deviceId = config.deviceId,
                voice = config.voice,
                autoStart = config.autoStart,
                locale = Locale(config.language, Locale.getDefault().country),
                attributes = Dynamic(
                        "clientType" to "standalone:${AppConfig.version}",
                        "clientScreen" to (config.screen != "none")
                )
        )
        if (config.introText != null)
            context.introText = config.introText!!
    }

    private fun createCallback() {
        callback = object : DeviceClientCallback(
                out,
                config.distUrl,
                config.autoUpdate,
                config.noCache,
                config.noOutputAudio,
                config.noOutputLogs,
                config.portName,
                logs = config.showLogs
        ) {
            override fun onBotStateChange(client: BotClient, newState: BotClient.State) {
                super.onBotStateChange(client, newState)
                when (newState) {
                    BotClient.State.Listening ->
                        if (light is ColorLight)
                            (light as ColorLight)?.set(Color.GREEN)
                        else
                            light?.high()
                    BotClient.State.Processing ->
                        if (light is ColorLight)
                            (light as ColorLight)?.set(Color.BLUE)
                    BotClient.State.Failed ->
                        if (light is ColorLight)
                            (light as ColorLight)?.set(Color.RED)
                    BotClient.State.Open -> {}
                    else ->
                        light?.low()
                }
            }

            override fun onReady(client: BotClient) {
                super.onReady(client)
                light?.apply {
                    blink(0)
                    low()
                }
            }

            override fun text(client: BotClient, text: String) {
                super.text(client, text)
                responded = true
            }

            override fun onFailure(client: BotClient, t: Throwable) {
                super.onFailure(client, t)
                responded = true
            }

            override fun onSessionId(client: BotClient, sessionId: String?) {
                super.onSessionId(client, sessionId)
                if (sessionId == null) {
                    responded = true
                    val version = config.version
                    loadConfig()
                    if (version != config.version) {
                        out.println("{Configuration version changed from $version to ${config.version} - exiting to be reloaded}")
                        Thread.sleep(5000)
                        exit()
                    }
                }
            }
        }
    }

    private fun setSpeechDevice() {
        speechDevice = SpeechDeviceFactory.getSpeechDevice(config.speechDevice)
    }

    private fun launchScreen() {
        if (config.screen != "none") {
            Screen.client = client
            Screen.fullScreen = (config.screen == "fullscreen")
            Screen.animations = !config.noAnimations
            thread {
                Screen.launch()
            }
        }
    }

    private fun createClient() {
        val micChannel = config.micChannel.split(':').map { it.toInt() }
        client = BotClient(
                context,
                when (config.socketType) {
                    BotSocketType.JWS -> JwsBotClientSocket(context.url, config.exitOnError, config.socketPing)
                    else -> OkHttp3BotClientSocket(context.url, config.exitOnError, config.socketPing)
                },
                if (config.noInputAudio)
                    null
                else
                    Microphone(speechDevice, config.wakeWord, micChannel[0], micChannel[1]),
                callback,
                if (config.noOutputAudio)
                    BotConfig.TtsType.None
                else
                    BotConfig.TtsType.RequiredLinks,
                config.ttsFileType,
                config.sttMode,
                config.pauseMode,
                if (config.noInputAudio || (config.audioRecordUpload == WavFileAudioRecorder.UploadMode.none))
                    null
                else
                    WavFileAudioRecorder(File("."),
                            if (context.url.startsWith("http://localhost"))
                                ServiceUrlResolver.getEndpointUrl("core", ServiceUrlResolver.RunMode.local)
                            else
                                context.url,
                            config.audioRecordUpload
                    )
        )
    }

    private fun enableSignalProcessing() = config.signalProcessor?.apply {
        out.println("{enabling signal processor}")
        emitter = { signalGroup: SignalGroup, values: PropertyMap ->
            when (signalGroup.type) {
                SignalGroup.Type.Text ->
                    if (client.state == BotClient.State.Sleeping) {
                        println("{Signal text '${signalGroup.name}' values $values}")
                        client.doText(signalGroup.name, values)
                    }
                SignalGroup.Type.Touch ->
                    client.touch()
            }
        }
        if (speechDevice is SignalProvider)
            providers.add(speechDevice as SignalProvider)
        run()
    }

    private fun enableHardware() {
        if (listOf("rpi", "model1", "model2", "model3").contains(config.device)) {
            val gpio = GpioFactory.getInstance()
            light = if (config.device == "model2")
                Vk2ColorLed().apply {
                    set(Color.MAGENTA)
                }
            else
                BinLed(gpio).apply {
                    blink(500)
                }
            val button = gpio.provisionDigitalInputPin(RaspiPin.GPIO_04, PinPullResistance.PULL_DOWN)
            button.setShutdownOptions(true)
            button.addListener(GpioPinListenerDigital { event ->
                when (event.state) {
                    PinState.LOW -> {
                        light?.high()
                        client.touch()
                    }
                    PinState.HIGH -> {
                        light?.low()
                    }
                }
            })
        }
    }

    private fun handleInput(input: String) {
        InputStreamReader(
                if (input == "stdin")
                    System.`in`
                else
                    FileInputStream(input)
        ).use {
            val input = BufferedReader(it)
            while (true) {
                val text = input.readLine()!!.trim()
                client.outputQueue.clear()
                when (text) {
                    "" -> {
                        println("[Click when ${client.state}]")
                        client.touch()
                    }
                    "exit", "quit" -> {
                        exit()
                    }
                    else -> {
                        if (text.startsWith("audio:"))
                            client.socket.sendAudioData(File(text.substring(6)).readBytes())
                        else if (client.state == BotClient.State.Responding || client.state == BotClient.State.Listening) {
                            responded = false
                            client.doText(text)
                        }
                    }
                }
                while (!responded && client.state != BotClient.State.Failed) {
                    Thread.sleep(50)
                }
            }
        }
    }

    override fun run(globalConfig: Application.Config, config: Config) {
        (LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger).level = Level.toLevel(globalConfig.logLevel)
        this.config = config

        setOutput()
        loadConfig()
        setVolume()
        createContext()
        createCallback()
        setSpeechDevice()
        createClient()
        launchScreen()
        enableSignalProcessing()
        enableHardware()

        out.apply {
            println("{context = $context}")
            println("{inputAudioDevice = ${client.inputAudioDevice}}")
            println("{sttMode = ${client.sttMode}}")
            println("{device = ${config.device}}")
        }
        client.open()
        if (config.input != "none")
            handleInput(config.input)
    }
}