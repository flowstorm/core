package ai.flowstorm.core.stt

import com.google.api.gax.rpc.ResponseObserver
import com.google.api.gax.rpc.StreamController
import com.google.cloud.speech.v1.StreamingRecognizeResponse
import ai.flowstorm.core.Input
import java.time.ZoneId
import java.util.*

class GoogleSttObserver(private val callback: SttCallback, private val locale: Locale, private val zoneId: ZoneId, private val singleUtterance: Boolean = false) : ResponseObserver<StreamingRecognizeResponse> {

    override fun onStart(controller: StreamController) {
        callback.onOpen()
    }

    override fun onResponse(response: StreamingRecognizeResponse?) {
        if (response == null)
            return

        if (singleUtterance && response.speechEventType == StreamingRecognizeResponse.SpeechEventType.END_OF_SINGLE_UTTERANCE) {
            callback.onEndOfUtterance()
        }

        for (result in response.resultsList) {
            val alternatives = result.alternativesList ?: continue
            val firstAlternative = alternatives[0]
            val input = Input(locale, zoneId, Input.Transcript(firstAlternative.transcript, firstAlternative.confidence))
            firstAlternative.wordsList.forEach {
                input.tokens.add(Input.Word(
                        it.word,
                        startTime = it.startTime.seconds.toFloat() + it.startTime.nanos.toFloat() / 1000000000,
                        endTime = it.endTime.seconds.toFloat() + it.endTime.nanos.toFloat() / 1000000000
                ))
            }
            for (i in 1 until alternatives.size) {
                val nextAlternative = alternatives[i]
                input.alternatives.add(Input.Transcript(nextAlternative.transcript, nextAlternative.confidence))
            }
            callback.onResponse(input, result.isFinal)
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