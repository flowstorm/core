package org.promethist.core.tts

import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.polly.AmazonPollyClient
import com.amazonaws.services.polly.model.OutputFormat
import com.amazonaws.services.polly.model.SynthesizeSpeechRequest
import com.amazonaws.services.polly.model.TextType
import org.promethist.common.AppConfig
import org.promethist.core.model.TtsConfig
import org.promethist.core.model.Voice
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream

object AmazonTtsService: TtsService {

    //val region = Region.getRegion(Regions.EU_WEST_1)
    private val client = AmazonPollyClient(
            /*DefaultAWSCredentialsProviderChain()*/
            //TODO set access + secret key from from environment
            AWSStaticCredentialsProvider(BasicAWSCredentials(AppConfig.instance["aws.access-key"], AppConfig.instance["aws.secret-key"])),
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
        val ttsRequest = TtsRequest(Voice.Audrey,
                """<speak><amazon:domain name="news">There has been a concerted effort among aides and allies to get President Donald Trump to stop conducting the daily coronavirus briefings, multiple sources tell CNN.</amazon:domain></speak>""",
                true
        )
        ByteArrayInputStream(speak(ttsRequest)).copyTo(FileOutputStream("/Users/tomas.zajicek/Downloads/test.mp3"))
    }
}