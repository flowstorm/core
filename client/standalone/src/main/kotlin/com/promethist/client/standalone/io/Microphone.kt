package com.promethist.client.standalone.io

import com.promethist.client.util.InputAudioDevice
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.TargetDataLine

class Microphone : InputAudioDevice() {

    val micFormat = AudioFormat(format.sampleRate.toFloat(), format.sampleSize, format.channels, true, false)
    val micInfo = DataLine.Info(TargetDataLine::class.java, micFormat)
    val micLine = AudioSystem.getLine(micInfo) as TargetDataLine
    override var bufferSize: Int = micLine.bufferSize / 5

    override fun start() {
        if (!started) {
            micLine.open(micFormat)
            micLine.start()
            super.start()
        }
    }

    override fun stop() {
        try {
            micLine.flush()
            micLine.close()
        } finally {
            super.stop()
        }
    }

    override fun read(buffer: ByteArray): Int = micLine.read(buffer, 0, buffer!!.size)
}