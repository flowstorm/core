package ai.flowstorm.core.tts

import ai.flowstorm.common.AppConfig
import ai.flowstorm.core.storage.FileStorage
import ai.flowstorm.util.LoggerDelegate
import java.io.ByteArrayOutputStream
import java.io.IOException
import javax.inject.Inject
import kotlin.concurrent.thread

class TtsAudioFileService {

    @Inject
    lateinit var fileStorage: FileStorage

    @Inject
    lateinit var ttsService: TtsService

    private val logger by LoggerDelegate()

    /**
     * Saves TTS audio to filestore for future usage.
     */
    private fun save(audioFile: TtsAudioFile) = with(audioFile) {
        fileStorage.writeFile(path, type, listOf("text:${request.text}"), data!!.inputStream())
    }

    /**
     * This creates and stores or loads existing audio from database cache for the specified TTS request.
     */
    @Throws(IOException::class)
    internal fun get(ttsRequest: TtsRequest, asyncSave: Boolean, download: Boolean): TtsAudioFile {
        val audio = TtsAudioFile(ttsService, ttsRequest)
        try {
            if (AppConfig.instance.get("tts.no-cache", "false") == "true")
                throw FileStorage.NotFoundException("no cache")
            val file = fileStorage.getFile(audio.path)
            logger.info("[HIT] ttsRequest=$ttsRequest")
            if (download) {
                ByteArrayOutputStream().apply {
                    fileStorage.readFile(audio.path, this)
                    audio.data = toByteArray()
                }
            }
        } catch (e: FileStorage.NotFoundException) {
            logger.info("[MISS] ttsRequest=$ttsRequest")
            audio.speak()
            if (asyncSave) {
                thread(start = true) {
                    save(audio)
                }
            } else {
                save(audio)
            }
            logger.info("[DONE]")
        }
        return audio
    }
}