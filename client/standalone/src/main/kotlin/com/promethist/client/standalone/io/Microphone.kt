package com.promethist.client.standalone.io

import com.promethist.client.audio.AudioDevice.Format
import com.promethist.client.audio.SnowboyWakeWordDetector
import com.promethist.client.util.InputAudioDevice
import com.promethist.client.audio.SpeechDevice
import com.promethist.client.audio.WakeWordConfig
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.TargetDataLine

class Microphone(speechDevice: SpeechDevice, wakeWord: WakeWordConfig? = null, private val channels: Int, private val channel: Int = 0) : InputAudioDevice(speechDevice) {

    private val format = AudioFormat(Format.DEFAULT.sampleRate.toFloat(), Format.DEFAULT.sampleSize, channels, true, false)
    private val sampleSize = format.sampleSizeInBits / 8
    private val info = DataLine.Info(TargetDataLine::class.java, format)
    private val line = AudioSystem.getLine(info) as TargetDataLine
    private val lineBuffer = ByteArray(line.bufferSize / 4)
    private val wakeWordDetector by lazy {
        when (wakeWord?.type ?: WakeWordConfig.Type.none) {
            WakeWordConfig.Type.snowboy -> SnowboyWakeWordDetector(wakeWord!!, lineBuffer.size / 2)
            WakeWordConfig.Type.none -> null
        }
    }

    override fun start() {
        if (!started) {
            line.open(format)
            line.start()
            super.start()
        }
    }

    override fun stop() {
        try {
            line.flush()
            line.close()
        } finally {
            super.stop()
        }
    }

    private fun readChannel(sourceBuffer: ByteArray, sourceCount: Int, buffer: ByteArray): Int {
        var count = 0
        for (i in 0 until sourceCount step channels * sampleSize) {
            buffer[count++] = sourceBuffer[i + channel * sampleSize]
            buffer[count++] = sourceBuffer[i + 1 + channel * sampleSize]
        }
        return count
    }

    override fun read(buffer: ByteArray): Int {
        val sourceCount = line.read(lineBuffer, 0, lineBuffer.size)
        val count = readChannel(lineBuffer, sourceCount, buffer)
        if (wakeWordDetector?.detect(buffer, count) == true)
            callback?.onWake()
        return count
    }

    override fun toString() = this::class.simpleName + "(speechDevice = ${speechDevice}, sampleRate = ${format.sampleRate}, sampleSize = $sampleSize, channels = $channels, channel = $channel)"
}