package ai.flowstorm.client.standalone.cli

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import com.pi4j.io.gpio.GpioFactory
import com.pi4j.io.gpio.PinPullResistance
import com.pi4j.io.gpio.PinState
import com.pi4j.io.gpio.RaspiPin
import com.pi4j.io.gpio.event.GpioPinListenerDigital
import cz.alry.jcommander.CommandRunner
import javazoom.jl.player.Player
import ai.flowstorm.client.audio.AudioCallback
import ai.flowstorm.client.audio.AudioDevice
import ai.flowstorm.client.gps.NMEA
import ai.flowstorm.client.signal.SignalProcessor
import ai.flowstorm.client.standalone.Application
import ai.flowstorm.client.standalone.io.Microphone
import ai.flowstorm.client.standalone.io.OutputAudioDevice
import ai.flowstorm.client.standalone.io.RespeakerMicArrayV2
import ai.flowstorm.client.standalone.io.SpeechDeviceFactory
import org.slf4j.LoggerFactory
import java.io.*
import java.util.*
import javax.sound.sampled.*

class ToolCommand: CommandRunner<Application.Config, ToolCommand.Config> {

    enum class Action { play, sample, audio, test, respeaker2, nmea, signal, props }

    private val BUF_SIZE = 3200

    @Parameters(commandNames = ["tool"], commandDescription = "Tool actions")
    class Config : ClientConfig() {

        @Parameter(names = ["-a", "--action"], order = 0, description = "Action")
        var action = Action.audio

        @Parameter(names = ["-m", "--microphone"], order = 1, description = "User microphone")
        var microphone = false
    }

    private fun play(config: Config) {
        val buf = ByteArrayOutputStream()
        var mic: Microphone? = null
        if (config.microphone) {
            val micChannel = config.micChannel.split(':').map { it.toInt() }
            mic = Microphone(SpeechDeviceFactory.getSpeechDevice(config.speechDevice), config.wakeWord, micChannel[0], micChannel[1])
            mic.callback = object : AudioCallback {
                override fun onStart() = println("Microphone started")
                override fun onStop() = println("Microphone stopped")
                override fun onData(data: ByteArray, size: Int): Boolean {
                    buf.write(data, 0, size)
                    return true
                }

                override fun onWake() {
                    println("Wake word detected")
                }
            }
            mic.start()
            Thread(mic).start()
            while (!mic.started)
                Thread.sleep(50)
        }
        if (config.input.endsWith(".mp3")) {
            // play MP3
            println("Playing from ${config.input}")
            Player(FileInputStream(config.input), object : OutputAudioDevice(config.speakerName) {
                override fun toByteArray(samples: ShortArray?, offs: Int, len: Int): ByteArray {
                    val b = super.toByteArray(samples, offs, len)
                    if (config.microphone)
                        buf.write(b, 0, len * 2)
                    return b
                }
            }).play()
        } else {
            // play PCM
            val audioFormat = AudioDevice.Format.DEFAULT
            val format = AudioFormat(audioFormat.sampleRate.toFloat(), audioFormat.sampleSize, audioFormat.channels, true, false)
            val pcm = File(config.input).readBytes()
            println("Playing PCM from ${config.input} (size ${pcm.size} bytes)")
            val info = DataLine.Info(SourceDataLine::class.java, format)
            val line = AudioSystem.getLine(info) as SourceDataLine
            line.open(format, BUF_SIZE)
            line.start()
            line.write(pcm, 0, pcm.size)
            line.drain()
            line.stop()
            line.close()
        }
        mic?.close(true)
        if (config.output != "stdout") {
            println("Writing PCM to ${config.output}")
            File(config.output).writeBytes(buf.toByteArray())
        }
    }

    class SpkLineQueue(private val spkLine: SourceDataLine): LinkedList<ByteArray>(), Runnable {
        var stop = false
        override fun run() {
            println("SpkLineQueue running")
            while (!stop) {
                if (!isEmpty()) {
                    val spkBuf = remove()
                    //println("playing ${spkBuf.size} bytes")
                    spkLine.write(spkBuf, 0, spkBuf.size)
                } else {
                    Thread.sleep(20)
                }
            }
            println("SpkLineQueue closing")
        }


    }

    private fun sample(config: Config) {
        // input PCM
        val inputFile = File(config.input)
        val inputStream = BufferedInputStream(FileInputStream(inputFile))
        val micFile = File("${config.output}.mic.pcm")
        val spkFile = File("${config.output}.spk.pcm")
        val micStream = BufferedOutputStream(FileOutputStream(micFile))
        val spkStream = BufferedOutputStream(FileOutputStream(spkFile))
        val inputSize = inputFile.length()
        val blockCount = (inputSize / BUF_SIZE).toInt()
        val format = AudioFormat(16000f, 16, 1, true, false)
        val zeroBuf = ByteArray(BUF_SIZE)

        val micInfo = DataLine.Info(TargetDataLine::class.java, format)
        val micLine = AudioSystem.getLine(micInfo) as TargetDataLine
        val micBuf = ByteArray(micLine.bufferSize / 5)
        val spkInfo = DataLine.Info(SourceDataLine::class.java, format)
        val spkLine = AudioSystem.getLine(spkInfo) as SourceDataLine

        println("OPENING LINES (micBuf ${micBuf.size}, spkBuf ${BUF_SIZE})")
        spkLine.open(format, BUF_SIZE)
        spkLine.start()
        micLine.open(format)
        micLine.start()

        val spkLineQueue = SpkLineQueue(spkLine)
        Thread(spkLineQueue).start()

        println("phase 1 - YOU")
        for (i in 0 until blockCount) {
            val c = micLine.read(micBuf, 0, micBuf.size)
            micStream.write(micBuf, 0, c)
            spkStream.write(zeroBuf, 0, c)
            println("phase 1 - $i / $blockCount ($c bytes)")
        }

        println("phase 2 - BOT")
        val spkBlocks = mutableListOf<ByteArray>()
        for (i in 0 until blockCount) {
            val spkBuf = ByteArray(BUF_SIZE)
            val c = micLine.read(micBuf, 0, micBuf.size)
            val c2 = inputStream.read(spkBuf)
            spkLineQueue.add(spkBuf.copyOfRange( 0, c))
            micStream.write(micBuf, 0, c)
            spkStream.write(spkBuf, 0, c2)
            spkBlocks.add(spkBuf)
            println("phase 2 - $i / $blockCount ($c bytes)")
        }

        println("phase 3 - BOTH")
        for (i in 0 until blockCount) {
            val spkBuf = spkBlocks.get(i)
            val c = micLine.read(micBuf, 0, micBuf.size)
            spkLineQueue.add(spkBuf.copyOfRange( 0, c))
            micStream.write(micBuf, 0, c)
            spkStream.write(spkBuf, 0, c)
            println("phase 3 - $i / $blockCount ($c bytes)")
        }

        println("CLOSING")
        spkLineQueue.stop = true
        spkLine.drain()
        micLine.drain()
        spkLine.stop()
        micLine.stop()
        spkLine.close()
        micLine.close()
        micStream.close()
        spkStream.close()
        inputStream.close()
    }

    private fun audio() {
        for (mixerInfo in AudioSystem.getMixerInfo()) {
            println("MIXER ${mixerInfo}")
            val mixer = AudioSystem.getMixer(mixerInfo)
            for (info in mixer.sourceLineInfo) {
                println(" SOURCE $info")
            }
            for (info in mixer.targetLineInfo) {
                println(" TARGET $info")
            }
            println()
        }

        if (AudioSystem.isLineSupported(Port.Info.MICROPHONE)) {
            val line = AudioSystem.getLine(Port.Info.MICROPHONE)
            println("MICROPHONE line = line")
        } else {
            println("NO MICROPHONE")
        }
    }

    private fun test() {

        val gpio = GpioFactory.getInstance()
        val button = gpio.provisionDigitalInputPin(RaspiPin.GPIO_04, PinPullResistance.PULL_DOWN)
        button.setShutdownOptions(true, PinState.LOW)
        button.addListener(GpioPinListenerDigital { event ->
            when (event.state) {
                PinState.LOW -> {
                    println("LOW")
                }
                PinState.HIGH -> {
                    println("HIGH")
                }
            }
        })
        Thread.sleep(60000)
/*
        var channel = 1

        val src = FileInputStream("testmic6.pcm").readAllBytes()
        val dst = ByteArray(src.size / 6)
        var j = 0
        for (i in 0 until src.size step 12) {
            dst[j++] = src[i + channel * 2]
            dst[j++] = src[i + 1 + channel * 2]
        }
        File("testmicx.pcm").writeBytes(dst)
*/
    }

    override fun run(globalConfig: Application.Config, config: Config) {
        (LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger).level = Level.toLevel(globalConfig.logLevel)
        when (config.action) {
            //Action.tts -> tts(config)
            Action.respeaker2 -> RespeakerMicArrayV2.test()
            Action.audio -> audio()
            Action.test -> test()
            Action.signal -> SignalProcessor.test(config.input)
            Action.nmea -> NMEA.test(config.input)
            Action.sample -> sample(config)
            Action.play -> play(config)
            Action.props -> System.getProperties().forEach {
                println("${it.key} = ${it.value}")
            }
        }
    }
}