package ai.flowstorm.client.standalone.io

import javazoom.jl.player.JavaSoundAudioDevice
import javax.sound.sampled.*

open class OutputAudioDevice(private val mixerName: String? = null) : JavaSoundAudioDevice() {

    override fun getSourceLineInfo(): DataLine.Info =
            if (mixerName != null)
                AudioUtils.getSourceLineInfo(mixerName) as DataLine.Info
            else
                super.getSourceLineInfo()

    companion object {

        const val VOLUME_UP = 10
        const val VOLUME_DOWN = -10

        fun volume(portName: String, value: Int): Int {

            val portInfo = Port.Info(Port::class.java, portName, false)
            if (AudioSystem.isLineSupported(portInfo)) {
                try {
                    val port = AudioSystem.getLine(portInfo) as Port
                    port.open()
                    val volumeControl = port.getControl(FloatControl.Type.VOLUME) as FloatControl
                    volumeControl.value = when (value) {
                        VOLUME_UP -> volumeControl.value * 10 + 1
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
}