package com.promethistai.port.tts

import org.slf4j.LoggerFactory
import java.io.File

object TtsServiceFactory {

    val logger = LoggerFactory.getLogger(TtsServiceFactory::class.qualifiedName)

    private fun get(provider: TtsConfig.Provider): TtsService =
        when (provider) {
            TtsConfig.Provider.Google -> GoogleTtsService
            TtsConfig.Provider.Amazon -> AmazonTtsService
            TtsConfig.Provider.Microsoft -> MicrosoftTtsService
        }

    fun speak(request: TtsRequest): ByteArray = get(TtsConfig.forVoice(request.voice).provider).speak(request)

    @JvmStatic
    fun main(args: Array<String>) {
        val speech = speak(TtsRequest(TtsConfig.Voice.Gabriela,"Třistatřicetři stříbrných stříkaček stříkalo přes třistatřicetři stříbrných střech."))
        File("local/speech.mp3").writeBytes(speech)
    }

}