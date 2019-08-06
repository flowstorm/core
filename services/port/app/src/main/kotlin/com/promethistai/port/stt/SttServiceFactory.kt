package com.promethistai.port.stt

import java.io.File

object SttServiceFactory {

    fun create(provider: String, config: SttConfig, callback: SttCallback): SttService {
        when (provider) {
            "google" -> return GoogleSttService(config, callback)
            else -> throw NotImplementedError()
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val speech = File("local/speech.mp3").readBytes()
        val client = create("google", SttConfig("cs-CZ", 44100), object : SttCallback {
            override fun onResponse(transcript: String, confidence: Float, final: Boolean) {
                println("SST response - transcript: $transcript, confidence: $confidence, final: $final")
            }

            override fun onOpen() {
                println("SST open")
            }

            override fun onError(e: Throwable) {
                println("SST error")
                e.printStackTrace()
            }
        }).createStream().write(speech, 0, speech.size)

        Thread.sleep(10000)
    }

}