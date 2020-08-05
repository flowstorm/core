package com.promethist.port.stt

import com.google.cloud.speech.v1.RecognitionConfig
import com.google.cloud.speech.v1.SpeechClient
import com.google.cloud.speech.v1.SpeechContext
import com.promethist.common.AppConfig
import com.promethist.core.ExpectedPhrase
import java.util.concurrent.TimeUnit

class GoogleSttService(private val callback: SttCallback) : SttService {

    private val client = SpeechClient.create()

    override fun createStream(config: SttConfig, expectedPhrases: List<ExpectedPhrase>): GoogleSttStream {
        val singleUtterance = AppConfig.instance.getOrNull("google.stt.singleUtterance") == "true"

        val recognitionConfig = RecognitionConfig.newBuilder()
                .setEncoding(when (config.encoding) {
                    SttConfig.Encoding.MULAW -> RecognitionConfig.AudioEncoding.MULAW
                    else -> RecognitionConfig.AudioEncoding.LINEAR16
                })
                .setLanguageCode(config.locale.language)
                .setSampleRateHertz(config.sampleRate)
                .setMaxAlternatives(5)
                .setEnableWordTimeOffsets(true)
                .addSpeechContexts(SpeechContext.newBuilder()
                        .addAllPhrases(expectedPhrases.map { it.text })
                        .build())
                .buildPartial()
        //		            .setModel("default")
        //		            .build();
        val observer = GoogleSttObserver(callback, config.locale, config.zoneId, singleUtterance)
        val stream = client.streamingRecognizeCallable().splitCall(observer)
        return GoogleSttStream.create(stream, recognitionConfig, singleUtterance)
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