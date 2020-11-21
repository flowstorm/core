package com.promethist.core.tts

import com.promethist.core.model.TtsConfig
import com.promethist.core.model.Voice
import com.promethist.util.LoggerDelegate
import java.io.File

object TtsServiceFactory {

    val logger by LoggerDelegate()

    private fun get(provider: TtsConfig.Provider): TtsService =
        when (provider) {
            TtsConfig.Provider.Google -> GoogleTtsService
            TtsConfig.Provider.Amazon -> AmazonTtsService
            TtsConfig.Provider.Microsoft -> MicrosoftTtsService
            TtsConfig.Provider.Voicery -> VoiceryTtsService
        }

    fun speak(request: TtsRequest): ByteArray {
        if (request.isSsml && !request.text.startsWith("<speak>"))
            request.text = "<speak>${request.text}</speak>"
        return get(TtsConfig.forVoice(request.voice).provider).speak(request)
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val speech = speak(TtsRequest(Voice.Gabriela, "Třistatřicetři stříbrných stříkaček stříkalo přes třistatřicetři stříbrných střech."))
        File("local/speech.mp3").writeBytes(speech)
    }

}