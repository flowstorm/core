package org.promethist.core.tts

import org.promethist.common.AppConfig
import org.promethist.core.storage.FileStorage
import org.promethist.util.LoggerDelegate
import java.io.ByteArrayOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.ws.rs.NotFoundException
import kotlin.concurrent.thread

class TtsAudioService {

    @Inject
    lateinit var fileStorage: FileStorage

    private val logger by LoggerDelegate()

    /**
     * Saves TTS audio to filestorefor future usage.
     */
    fun set(code: String, type: String, data: ByteArray, ttsRequest: TtsRequest) {
        logger.info("set(code=$code, fileType=$type, data[${data.size}])")
        fileStorage.writeFile("tts/${ttsRequest.voice}/$code.mp3", type, listOf("text:${ttsRequest.text}"), data.inputStream())
    }

    /**
     * This creates and stores or loads existing audio from database cache for the specified TTS request.
     */
    @Throws(IOException::class)
    internal fun get(ttsRequest: TtsRequest, asyncSave: Boolean, download: Boolean): TtsAudio {
        val audio = TtsAudio(ttsRequest)
        val path = "tts/${ttsRequest.voice}/${audio.code}.mp3"
        try {
            if (AppConfig.instance.get("tts.no-cache", "false") == "true")
                throw NotFoundException("tts.no-cache=true")
            val ttsFile = fileStorage.getFile(path)
            logger.info("[HIT] get ttsRequest=$ttsRequest")
            if (download) {
                ByteArrayOutputStream().apply {
                    fileStorage.readFile(path, this)
                    audio.data = toByteArray()
                }
            }
            audio.path = path
        } catch (e: FileStorage.NotFoundException) {
            logger.info("[MISS] get ttsRequest=$ttsRequest")
            audio.speak()
            logger.info("[DONE] get")
            if (asyncSave) {
                thread(start = true) {
                    set(audio.code, audio.type, audio.data!!, ttsRequest)
                }
            } else {
                set(audio.code, audio.type, audio.data!!, ttsRequest)
                audio.path = path
            }
        }
        return audio
    }
}