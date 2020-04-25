package com.promethist.port.tts

import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.polly.AmazonPollyClient
import com.amazonaws.services.polly.model.OutputFormat
import com.amazonaws.services.polly.model.SynthesizeSpeechRequest
import com.amazonaws.services.polly.model.TextType
import com.promethist.core.model.TtsConfig
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream

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
        val request = SynthesizeSpeechRequest()
                .withText(ttsRequest.text)
                .withTextType(if (ttsRequest.isSsml) TextType.Ssml else TextType.Text)
                .withVoiceId(ttsConfig.name)
                .withOutputFormat(OutputFormat.Mp3)
        request.engine = "neural"
        val result = client.synthesizeSpeech(request)
        result.audioStream.copyTo(buf)
        buf.close()
        return buf.toByteArray()
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val ttsRequest = TtsRequest("Audrey",
                """<speak><amazon:domain name="news">There has been a concerted effort among aides and allies to get President Donald Trump to stop conducting the daily coronavirus briefings, multiple sources tell CNN.</amazon:domain></speak>""",
                true
        )
        ByteArrayInputStream(speak(ttsRequest)).copyTo(FileOutputStream("/Users/tomas.zajicek/Downloads/test.mp3"))
    }
}