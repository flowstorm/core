package com.promethist.port.stt

import com.google.api.gax.rpc.ClientStream
import com.google.cloud.speech.v1.RecognitionConfig
import com.google.cloud.speech.v1.StreamingRecognitionConfig
import com.google.cloud.speech.v1.StreamingRecognizeRequest
import com.google.protobuf.ByteString

class GoogleSttStream(private val clientStream: ClientStream<StreamingRecognizeRequest>) : SttStream {

    override fun write(data: ByteArray, offset: Int, size: Int) {
        val request = StreamingRecognizeRequest.newBuilder()
                .setAudioContent(ByteString.copyFrom(data, offset, size))
                .build()
        clientStream.send(request)
    }

    override fun close() {
        clientStream.closeSend()
    }

    companion object {

        fun create(clientStream: ClientStream<StreamingRecognizeRequest>, recognitionConfig: RecognitionConfig, singleUtterance: Boolean = false): GoogleSttStream {
            val streamingRecognitionConfig =
                    StreamingRecognitionConfig.newBuilder().apply {
                        setConfig(recognitionConfig)
                        this.singleUtterance = singleUtterance
                    }.buildPartial()
            val request = StreamingRecognizeRequest.newBuilder()
                    .setStreamingConfig(streamingRecognitionConfig)
                    .buildPartial() // The first request in a streaming call has to be a configuration
            clientStream.send(request)
            return GoogleSttStream(clientStream)
        }
    }
}