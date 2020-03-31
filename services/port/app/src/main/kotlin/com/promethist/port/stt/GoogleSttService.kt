package com.promethist.port.stt

import com.google.cloud.speech.v1.RecognitionConfig
import com.google.cloud.speech.v1.SpeechClient
import com.google.cloud.speech.v1.SpeechContext
import com.promethist.core.model.Message
import java.util.concurrent.TimeUnit

class GoogleSttService(config: SttConfig, callback: SttCallback, expectedPhrases: List<Message.ExpectedPhrase>) : SttService {

    private val client = SpeechClient.create()
    private val responseObserver = GoogleSttObserver(callback, config.language?:"en")
    private val recognitionConfig = RecognitionConfig.newBuilder()
            .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
            .setLanguageCode(config.language)
            .setSampleRateHertz(config.sampleRate)
            .setMaxAlternatives(5)
            .setEnableWordTimeOffsets(true)
            .addSpeechContexts(SpeechContext.newBuilder()
                    .addAllPhrases(expectedPhrases.map { expectedPhrase: Message.ExpectedPhrase -> expectedPhrase.text })
                    .build())
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