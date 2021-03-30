package ai.flowstorm.core.util

import ai.flowstorm.common.SystemUtil.exec
import ai.flowstorm.core.AudioFileType
import ai.flowstorm.security.Digest.md5
import ai.flowstorm.util.LoggerDelegate
import java.io.File

object AudioUtil {

    private const val ffmpeg = "/usr/local/bin/ffmpeg"
    private val workDir = File(System.getProperty("java.io.tmpdir"))
    private val logger by LoggerDelegate()

    fun convert(data: ByteArray, fileType: AudioFileType, hash: String = md5(data)): ByteArray {
        val destFile = File(workDir, "$hash.${fileType.name}")
        if (!destFile.exists()) {
            logger.info("Generating $destFile")
            val sourceFile = File(workDir, "$hash.mp3")
            sourceFile.writeBytes(data)
            when (fileType) {
                AudioFileType.mulaw -> exec("$ffmpeg -y -i ${sourceFile.absolutePath} -codec:a pcm_mulaw -ar 8k -ac 1 -f mulaw ${destFile.absolutePath}")
                AudioFileType.wav -> exec("$ffmpeg -y -i ${sourceFile.absolutePath} -acodec pcm_s16le -ar 22050 -ac 1 ${destFile.absolutePath}")
                else -> error("Unsupported TTS conversation to $fileType")
            }
        }
        return destFile.readBytes()
    }
}