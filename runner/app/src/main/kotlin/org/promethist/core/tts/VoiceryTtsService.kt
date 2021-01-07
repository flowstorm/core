package org.promethist.core.tts

import org.promethist.common.AppConfig
import org.promethist.common.RestClient
import org.promethist.core.model.TtsConfig
import org.promethist.core.model.Voice
import org.promethist.util.LoggerDelegate
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.io.InputStream
import javax.ws.rs.client.Entity

object VoiceryTtsService : TtsService {

    private val logger by LoggerDelegate()

    override fun speak(ttsRequest: TtsRequest): ByteArray {
        val buf = ByteArrayOutputStream()
        val req = mutableMapOf(
                "text" to ttsRequest.text,
                "ssml" to ttsRequest.isSsml,
                "speaker" to TtsConfig.forVoice(ttsRequest.voice).name,
                "sampleRate" to ttsRequest.sampleRate
        ).apply {
            if (ttsRequest.style.isNotBlank())
                this["style"] = ttsRequest.style
        }
        val res = RestClient.webTarget("https://voicery.com/generate").request()
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + AppConfig.instance["voicery.key"])
                .post(Entity.json(req))
        if (res.status != 200) {
            logger.error(res.readEntity(String::class.java))
            error(res.statusInfo.reasonPhrase)
        }
        res.readEntity(InputStream::class.java).use {
            it.copyTo(buf)
        }
        return buf.toByteArray()
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val ttsRequest = TtsRequest(Voice.Victoria,
                """<speak>Hello world from Voicery! How are you?</speak>""",
                true
        )
        ByteArrayInputStream(speak(ttsRequest)).copyTo(FileOutputStream("/Users/tomas.zajicek/Downloads/voicery.mp3"))
    }
}