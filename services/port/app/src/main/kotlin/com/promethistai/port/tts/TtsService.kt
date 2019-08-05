package com.promethistai.port.tts

import com.promethistai.port.tts.impl.GoogleTtsService
import java.io.Closeable

interface TtsService: Closeable {

    val voices: List<TtsVoice>

    fun speak(text: String, voiceName: String, language: String): ByteArray

    companion object {

        fun create(provider: String = "google"): TtsService {
            when (provider) {
                "google" -> return GoogleTtsService()
                else -> throw NotImplementedError()
            }
        }
        /*
        @JvmStatic
        fun main(args: Array<String>) {
            val speech = create("google").speak("Třistatřicetři stříbrných stříkaček stříkalo přes třistatřicetři stříbrných střech.", "cs-CZ-Standard-A", "cs-CZ")
            File("local/speech.mp3").writeBytes(speech)
        }

         */
    }
}