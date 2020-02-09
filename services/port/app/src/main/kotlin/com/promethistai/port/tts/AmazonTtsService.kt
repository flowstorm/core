package com.promethistai.port.tts

import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.services.polly.AmazonPollyClient
import com.amazonaws.services.polly.model.OutputFormat
import com.amazonaws.services.polly.model.SynthesizeSpeechRequest
import com.amazonaws.services.polly.model.TextType
import java.io.ByteArrayOutputStream

object AmazonTtsService: TtsService {

    //val region = Region.getRegion(Regions.EU_WEST_1)
    private val client = AmazonPollyClient(DefaultAWSCredentialsProviderChain(), ClientConfiguration())

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