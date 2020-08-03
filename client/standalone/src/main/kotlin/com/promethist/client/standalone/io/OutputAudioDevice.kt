package com.promethist.client.standalone.io

import javax.sound.sampled.*

object OutputAudioDevice {

    const val VOLUME_UP = 10
    const val VOLUME_DOWN = -10

    fun volume(portName: String, value: Int): Int {
        val source = Port.Info(Port::class.java, portName, false)
        if (AudioSystem.isLineSupported(source)) {
            try {
                val outline = AudioSystem.getLine(source) as Port
                outline.open()
                val volumeControl = outline.getControl(FloatControl.Type.VOLUME) as FloatControl
                volumeControl.value = when (value) {
                    VOLUME_UP -> volumeControl.value * 10+ 1
                    VOLUME_DOWN -> volumeControl.value * 10 - 1
                    else -> value
                } as Float / 10F
                return (volumeControl.value * 10).toInt()
            } catch (ex: LineUnavailableException) {
                error("Audio source not supported")
                ex.printStackTrace()
            }
        }
        return 0
    }
}