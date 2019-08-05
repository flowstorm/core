package com.promethistai.port.stt.impl

import com.google.cloud.speech.v1.RecognitionConfig
import com.google.cloud.speech.v1.SpeechClient
import com.promethistai.port.stt.SttConfig
import com.promethistai.port.stt.SttCallback
import com.promethistai.port.stt.SttService
import java.util.concurrent.TimeUnit

class GoogleSttService(config: SttConfig, callback: SttCallback) : SttService {

    private val client = SpeechClient.create()
    private val responseObserver = GoogleSttObserver(callback)
    private val recognitionConfig = RecognitionConfig.newBuilder()
            .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
            .setLanguageCode(config.language)
            .setSampleRateHertz(config.sampleRate)
            .buildPartial()
    //		            .setModel("default")
    //		            .build();

    override fun createStream(): GoogleSttStream {
        val clientStream = client.streamingRecognizeCallable().splitCall(responseObserver)
        return GoogleSttStream.create(clientStream, recognitionConfig)
    }

    override fun close() {
        try {
            client.awaitTermination(5, TimeUnit.SECONDS)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        client.close()
    }

}