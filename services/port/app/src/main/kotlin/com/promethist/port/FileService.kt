package com.promethist.port

import com.promethist.common.AppConfig
import com.promethist.core.resources.FileResource
import com.promethist.port.tts.TtsRequest
import com.promethist.port.tts.TtsServiceFactory
import com.promethist.util.LoggerDelegate
import java.io.*
import javax.inject.Inject
import javax.ws.rs.NotFoundException
import kotlin.concurrent.thread

class FileService {

    inner class TtsAudio(val ttsRequest: TtsRequest) {

        val code = ttsRequest.code()
        var type = "audio/mpeg"
        var data: ByteArray? = null
        var path: String? = null

        /**
         * Returns or generates audio data if not already set.
         */
        fun speak(): TtsAudio {
            if (data == null) {
                try {
                    data = TtsServiceFactory.speak(ttsRequest)
                } catch (e: Throwable) {
                    throw IOException(e.message, e)
                }
            }
            return this
        }
    }

    @Inject
    lateinit var fileResource: FileResource

    private val logger by LoggerDelegate()

    /**
     * Saves TTS audio to filestorefor future usage.
     */
    fun saveTtsAudio(code: String, type: String, data: ByteArray, ttsRequest: TtsRequest) {
        logger.info("saveTtsAudio(code = $code, fileType = $type, data[${data.size}])")
        fileResource.writeFile("tts/${ttsRequest.voice}/$code.mp3", type, listOf("text:${ttsRequest.text}"), data.inputStream())
    }

    /**
     * This creates and stores or loads existing audio from database cache for the specified TTS request.
     */
    @Throws(IOException::class)
    internal fun getTtsAudio(ttsRequest: TtsRequest, asyncSave: Boolean, download: Boolean): TtsAudio {
        val audio = TtsAudio(ttsRequest)
        val path = "tts/${ttsRequest.voice}/${audio.code}.mp3"
        try {
            if (AppConfig.instance.get("tts.no-cache", "false") == "true")
                throw NotFoundException("tts.no-cache = true")
            val ttsFile = fileResource.getFile(path)
            logger.info("getTtsAudio[HIT](ttsRequest = $ttsRequest)")
            if (download)
                audio.data = fileResource.readFile(path).readEntity(ByteArray::class.java)
            audio.path = path
        } catch (e: NotFoundException) {
            logger.info("getTtsAudio[MISS](ttsRequest = $ttsRequest)")
            audio.speak() // perform speech synthesis
            logger.info("getTtsAudio[DONE]")
            if (asyncSave) {
                thread(start = true) {
                    saveTtsAudio(audio.code, audio.type, audio.data!!, ttsRequest)
                }
            } else {
                saveTtsAudio(audio.code, audio.type, audio.data!!, ttsRequest)
                audio.path = path
            }
        }
        return audio
    }
}