package com.promethist.core.tts

import com.promethist.common.AppConfig
import com.promethist.common.RestClient
import com.promethist.common.ServiceUrlResolver
import com.promethist.core.resources.FileResource
import com.promethist.util.LoggerDelegate
import java.io.IOException
import javax.ws.rs.NotFoundException
import kotlin.concurrent.thread

object TtsAudioService {

    private val fileResource = RestClient.instance(FileResource::class.java, ServiceUrlResolver.getEndpointUrl("core", ServiceUrlResolver.RunMode.local) + "/file")
    private val logger by LoggerDelegate()

    /**
     * Saves TTS audio to filestorefor future usage.
     */
    fun set(code: String, type: String, data: ByteArray, ttsRequest: TtsRequest) {
        logger.info("saveTtsAudio(code = $code, fileType = $type, data[${data.size}])")
        fileResource.writeFile("tts/${ttsRequest.voice}/$code.mp3", type, listOf("text:${ttsRequest.text}"), data.inputStream())
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