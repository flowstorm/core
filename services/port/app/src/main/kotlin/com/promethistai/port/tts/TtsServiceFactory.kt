package com.promethistai.port.tts

import com.promethistai.port.tts.impl.GoogleTtsService
import java.io.File

object TtsServiceFactory {


    fun create(provider: String = "google"): TtsService {
        when (provider) {
            "google" -> return GoogleTtsService()
            else -> throw NotImplementedError()
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val speech = create("google").speak("Třistatřicetři stříbrných stříkaček stříkalo přes třistatřicetři stříbrných střech.", TtsConfig.DEFAULT_CS)
        File("local/speech.mp3").writeBytes(speech)
    }

}