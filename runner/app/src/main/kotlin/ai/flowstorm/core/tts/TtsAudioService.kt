package ai.flowstorm.core.tts

import ai.flowstorm.common.config.ConfigValue
import ai.flowstorm.core.AudioFileType
import ai.flowstorm.core.storage.FileStorage
import ai.flowstorm.util.LoggerDelegate
import java.io.ByteArrayOutputStream
import java.io.IOException
import javax.inject.Inject
import kotlin.concurrent.thread

class TtsAudioService {

    @ConfigValue("tts.no-cache", "false")
    lateinit var ttsNoCache: String

    @Inject
    lateinit var fileStorage: FileStorage

    @Inject
    lateinit var ttsService: TtsService

    private val logger by LoggerDelegate()

    /**
     * Saves TTS audio to filestore for future usage.
     */
    private fun save(audioFile: TtsAudioFile) = with(audioFile) {
        fileStorage.writeFile(path, fileType.contentType, listOf("text:${request.text}"), data!!.inputStream())
    }

    /**
     * This creates and stores or loads existing audio from database cache for the specified TTS request.
     */
    @Throws(IOException::class)
    internal fun get(request: TtsRequest, fileType: AudioFileType, asyncSave: Boolean, download: Boolean): TtsAudioFile {
        val audioFile = TtsAudioFile(ttsService, request, fileType)
        try {
            if (ttsNoCache == "true")
                throw FileStorage.NotFoundException("Bypassing TTS cache")
            val fileObject = fileStorage.getFile(audioFile.path)
            logger.info("HIT ${audioFile.path} $request")
            if (download) {
                ByteArrayOutputStream().apply {
                    fileStorage.readFile(audioFile.path, this)
                    audioFile.data = toByteArray()
                }
            }
        } catch (e: FileStorage.NotFoundException) {
            logger.info("MISS ${audioFile.path} $request")
            audioFile.speak()
            if (asyncSave) {
                thread(start = true) {
                    save(audioFile)
                }
            } else {
                save(audioFile)
            }
            logger.info("DONE ${audioFile.path}")
        }
        return audioFile
    }
}