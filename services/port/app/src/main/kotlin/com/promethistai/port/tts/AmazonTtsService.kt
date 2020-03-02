package com.promethistai.port.tts

import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.polly.AmazonPollyClient
import com.amazonaws.services.polly.model.OutputFormat
import com.amazonaws.services.polly.model.SynthesizeSpeechRequest
import com.amazonaws.services.polly.model.TextType
import com.promethistai.core.model.TtsConfig
import java.io.ByteArrayOutputStream

object AmazonTtsService: TtsService {

    //val region = Region.getRegion(Regions.EU_WEST_1)
    private val client = AmazonPollyClient(
            /*DefaultAWSCredentialsProviderChain()*/
            //TODO set access + secret key from from environment
            AWSStaticCredentialsProvider(BasicAWSCredentials("AKIAJF7AJY2YPGYWPRCQ", "oHb3U5ikYoZDsFgAUAbF1L1oAa8t/5XDWNr7KXDN")),
            ClientConfiguration())

    override fun speak(ttsRequest: TtsRequest): ByteArray {
        val ttsConfig = TtsConfig.forVoice(ttsRequest.voice)
        val buf = ByteArrayOutputStream()
        val result = client.synthesizeSpeech(
            SynthesizeSpeechRequest()
                .withText(ttsRequest.text)
                .withTextType(if (ttsRequest.isSsml) TextType.Ssml else TextType.Text)
                .withVoiceId(ttsConfig.name)
                .withOutputFormat(OutputFormat.Mp3)
        )
        result.audioStream.copyTo(buf)
        buf.close()
        return buf.toByteArray()
    }
}