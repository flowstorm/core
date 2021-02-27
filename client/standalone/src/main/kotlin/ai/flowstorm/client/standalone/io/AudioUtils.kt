package ai.flowstorm.client.standalone.io

import javax.sound.sampled.AudioSystem

object AudioUtils {

    fun getMixer(name: String) =
            AudioSystem.getMixerInfo().firstOrNull { it.name == name }?.let { info ->
                AudioSystem.getMixer(info)
            } ?: error("no mixer info $name found")

    fun getSourceLineInfo(mixerName: String, index: Int = 0) = getMixer(mixerName).sourceLineInfo[index]

    fun getTargetLineInfo(mixerName: String, index: Int = 0) = getMixer(mixerName).targetLineInfo[index]
}