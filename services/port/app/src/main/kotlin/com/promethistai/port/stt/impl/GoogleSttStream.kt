package com.promethistai.port.stt.impl

import com.google.api.gax.rpc.ClientStream
import com.google.cloud.speech.v1.RecognitionConfig
import com.google.cloud.speech.v1.StreamingRecognitionConfig
import com.google.cloud.speech.v1.StreamingRecognizeRequest
import com.google.protobuf.ByteString
import com.promethistai.port.stt.SttStream

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

        fun create(clientStream: ClientStream<StreamingRecognizeRequest>, recognitionConfig: RecognitionConfig): GoogleSttStream {
            val streamingRecognitionConfig =
                    StreamingRecognitionConfig.newBuilder().setConfig(recognitionConfig).buildPartial()
            val request = StreamingRecognizeRequest.newBuilder()
                    .setStreamingConfig(streamingRecognitionConfig)
                    .buildPartial() // The first request in a streaming call has to be a configuration
            clientStream.send(request)
            return GoogleSttStream(clientStream)
        }
    }
}