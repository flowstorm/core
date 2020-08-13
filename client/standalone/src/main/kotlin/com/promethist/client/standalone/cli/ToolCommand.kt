package com.promethist.client.standalone.cli

import com.promethist.client.standalone.Application
import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import com.promethist.client.standalone.io.Microphone
import com.promethist.client.util.AudioCallback
import com.promethist.client.util.AudioDevice
import com.promethist.core.model.TtsConfig
import cz.alry.jcommander.CommandRunner
import javazoom.jl.decoder.*
import javazoom.jl.player.JavaSoundAudioDevice
import javazoom.jl.player.Player
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import javax.sound.sampled.*


class ToolCommand: CommandRunner<Application.Params, ToolCommand.Params> {

    enum class Action { tts, voices, play, test }

    val BUF_SIZE = 3200

    @Parameters(commandNames = ["tool"], commandDescription = "Tool actions")
    class Params : ClientParams() {

        @Parameter(names = ["-a", "--action"], order = 0, description = "Action")
        var action = Action.tts

        @Parameter(names = ["-m", "--microphone"], order = 10, description = "Enable microphone")
        var microphone = false
    }

    /*
    fun tts(params: Params) {
        println("Processing TTS from \"${params.input}\" to ${params.output}")
        val port = RestClient.instance(PortResource::class.java, params.url)
        val data = port.tts(params.key, TtsRequest(TtsConfig.defaultVoice(params.language), params.input!!))
        File(params.output).writeBytes(data)
    }
    */
    fun play(params: Params) {
        val buf = ByteArrayOutputStream()
        var mic: Microphone? = null
        if (params.microphone) {
            mic = Microphone()
            mic.callback = object : AudioCallback {
                override fun onStart() = println("Microphone started (buffer size = ${mic.bufferSize})")
                override fun onStop() = println("Microphone stopped")
                override fun onData(data: ByteArray, size: Int): Boolean {
                    buf.write(data, 0, size)
                    return true
                }
            }
            mic.start()
            Thread(mic).start()
            while (!mic.started)
                Thread.sleep(50)
        }
        if (params.input.endsWith(".mp3")) {
            // play MP3
            println("Playing from ${params.input}")
            Player(FileInputStream(params.input), object : JavaSoundAudioDevice() {
                override fun toByteArray(samples: ShortArray?, offs: Int, len: Int): ByteArray {
                    val b = super.toByteArray(samples, offs, len)
                    if (!params.microphone)
                        buf.write(b, 0, len * 2)
                    return b
                }
            }).play()
            val b: Bitstream
        } else {
            // play PCM
            val audioFormat = AudioDevice.Format.DEFAULT
            val format = AudioFormat(audioFormat.sampleRate.toFloat(), audioFormat.sampleSize, audioFormat.channels, true, false)
            val pcm = File(params.input).readBytes()
            println("Playing PCM from ${params.input} (size ${pcm.size} bytes)")
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
        if (params.output != "stdout") {
            println("Writing PCM to ${params.output}")
            File(params.output).writeBytes(buf.toByteArray())
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

    fun test(params: Params) {
        // input PCM
        val inputFile = File(params.input)
        val inputStream = BufferedInputStream(FileInputStream(inputFile))
        val micFile = File("${params.output}.mic.pcm")
        val spkFile = File("${params.output}.spk.pcm")
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

    override fun run(globalParams: Application.Params, params: Params) {
        when (params.action) {
            //Action.tts -> tts(params)
            Action.voices -> TtsConfig.values.forEach { println(it) }
            Action.play -> play(params)
            Action.test -> test(params)
        }
    }

    companion object {

        val decoder = Decoder()

        fun recode(input: InputStream, output: ByteArrayOutputStream) {
            val bitStream = Bitstream(input)
            var done = false
            var i = 0
            while (!done) {
                val header: Header? = bitStream.readFrame()
                if (i++ == 0)
                    println(header)
                if (header == null) {
                    done = true
                } else {
                    val sample = decoder.decodeFrame(header, bitStream) as SampleBuffer
                    val buffer = ByteBuffer.allocate(sample.buffer.size * 2).order(ByteOrder.BIG_ENDIAN)
                    buffer.asShortBuffer().put(sample.buffer)
              }
                bitStream.closeFrame()
            }
        }
    }
}