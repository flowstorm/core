package com.promethist.port.stt

import com.google.api.gax.rpc.ResponseObserver
import com.google.api.gax.rpc.StreamController
import com.google.cloud.speech.v1.StreamingRecognizeResponse
import com.promethist.core.Input
import java.util.*

class GoogleSttObserver(private val callback: SttCallback, private val language: String) : ResponseObserver<StreamingRecognizeResponse> {

    private val responses = ArrayList<StreamingRecognizeResponse>()

    override fun onStart(controller: StreamController) {
        callback.onOpen()
    }

    override fun onResponse(response: StreamingRecognizeResponse?) {
        println("onResponse: $response")
        if (response == null || response.resultsList == null)
            return
        responses.add(response)
        for (result in response.resultsList) {
            val alternatives = result.alternativesList ?: continue
            for (alt in alternatives) {
                val input = Input(alt.transcript, Locale(language), confidence = alt.confidence)
                alt.wordsList.forEach {
                    val word = Input.Word(
                            it.word,
                            startTime = it.startTime.seconds.toFloat() + it.startTime.nanos.toFloat() / 1000000000,
                            endTime = it.endTime.seconds.toFloat() + it.endTime.nanos.toFloat() / 1000000000
                    )
                    input.tokens.add(word)
                }
                callback.onResponse(input, result.isFinal)
                alt.wordsList
            }
        }
    }

    override fun onComplete() {
        /*
        for (response in responses) {
            val result = response.getResultsList().get(0)
            val alternative = result.getAlternativesList().get(0)
            println("Transcript : ${alternative.transcript}\n")
        }*/
    }

    override fun onError(t: Throwable) {
        callback.onError(t)
    }
}