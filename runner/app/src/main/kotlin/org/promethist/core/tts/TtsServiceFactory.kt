package org.promethist.core.tts

import org.promethist.core.model.Voice
import org.promethist.util.LoggerDelegate
import java.io.File

object TtsServiceFactory {

    val logger by LoggerDelegate()

    private fun get(provider: String): TtsService =
        when (provider) {
            "Google" -> GoogleTtsService
            "Amazon" -> AmazonTtsService
            "Microsoft" -> MicrosoftTtsService
            "Voicery" -> VoiceryTtsService
            else -> error("Unknown TTS service provider: $provider")
        }

    fun speak(request: TtsRequest): ByteArray {
        if (request.isSsml && !request.text.startsWith("<speak>"))
            request.text = "<speak>${request.text}</speak>"
        return get(request.config.provider).speak(request)
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val speech = speak(TtsRequest(Voice.Gabriela.config, "Třistatřicetři stříbrných stříkaček stříkalo přes třistatřicetři stříbrných střech."))
        File("local/speech.mp3").writeBytes(speech)
    }

}