package com.promethist.port.stt

import com.google.api.gax.rpc.ResponseObserver
import com.google.api.gax.rpc.StreamController
import com.google.cloud.speech.v1.StreamingRecognizeResponse
import com.promethist.port.stt.SttCallback
import java.util.ArrayList

class GoogleSttObserver(private val callback: SttCallback) : ResponseObserver<StreamingRecognizeResponse> {

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
            for (alt in alternatives)
                callback.onResponse(alt.transcript, alt.confidence, result.isFinal)
        }
    }

    override fun onComplete() {/*
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