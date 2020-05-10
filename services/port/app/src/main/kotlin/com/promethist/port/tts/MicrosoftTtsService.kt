package com.promethist.port.tts

import com.microsoft.cognitiveservices.speech.*
import com.promethist.core.model.TtsConfig
import org.w3c.dom.Document
import java.io.ByteArrayInputStream
import java.io.File
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

object MicrosoftTtsService: TtsService {

    object XmlTransformer {

        private val xmlBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        private val xmlTransformer = TransformerFactory.newInstance().newTransformer()
        init {
            xmlTransformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
        }

        fun transform(input: String, callback: ((Document) -> Unit)): String {
            val xml = xmlBuilder.parse(ByteArrayInputStream(input.toByteArray()))
            callback(xml)
            val buf = StringWriter()
            xmlTransformer.transform(DOMSource(xml), StreamResult(buf))
            return buf.toString()
        }
    }

    fun createSynthesizer(voiceName: String): SpeechSynthesizer {
        //TODO set subcription key + region from environment
        val config: SpeechConfig = SpeechConfig.fromSubscription("3e54aecb6d564750bce2c5536c86727e", "westeurope")
        config.setSpeechSynthesisOutputFormat(SpeechSynthesisOutputFormat.Audio16Khz64KBitRateMonoMp3)
        config.speechSynthesisVoiceName = voiceName
        return SpeechSynthesizer(config)
    }

    override fun speak(ttsRequest: TtsRequest): ByteArray {
        val ttsConfig = TtsConfig.forVoice(ttsRequest.voice)
        val synthesizer = createSynthesizer(ttsConfig.name)
        val result = if (ttsRequest.isSsml) {
            // well.. we need to transform SSML to match Microsoft requirements
            ttsRequest.text = XmlTransformer.transform(ttsRequest.text) { ssml ->
                val speak = ssml.documentElement
                if (!speak.hasAttribute("version"))
                    speak.setAttribute("version", "1.0")
                if (!speak.hasAttribute("xmlns"))
                    speak.setAttribute("xmlns", "https://www.w3.org/2001/10/synthesis")
                if (!speak.hasAttribute("xml:lang"))
                    speak.setAttribute("xml:lang", ttsConfig.locale.toLanguageTag())
                if (speak.getElementsByTagName("voice").length == 0) {
                    val voice = ssml.createElement("voice")
                    voice.setAttribute("name", ttsConfig.name)
                    val childNodes = speak.childNodes
                    for (i in 0 until childNodes.length) {
                        val child = childNodes.item(i)
                        if (child != null) {
                            speak.removeChild(child)
                            voice.appendChild(child)
                        }
                    }
                    speak.appendChild(voice)
                }
            }
            TtsServiceFactory.logger.info("speak(ttsRequest = $ttsRequest)")
            synthesizer.SpeakSsml(ttsRequest.text)
        } else {
            TtsServiceFactory.logger.info("speak(ttsRequest = $ttsRequest)")
            synthesizer.SpeakText(ttsRequest.text)
        }
        synthesizer.close()
        //println("*** MS synth $ttsRequest [${result.audioData.size}] result = ${result.reason} ")

        if (result.reason == ResultReason.Canceled) {
            val cancellation = SpeechSynthesisCancellationDetails.fromResult(result)
            error(cancellation.errorDetails)
        }

        return result.audioData
    }

    @JvmStatic
    @Throws(Exception::class)
    fun main(args: Array<String>) {
        //val str = "<speak version=\"1.0\" xmlns=\"https://www.w3.org/2001/10/synthesis\" xml:lang=\"en-US\"><voice name=\"en-US-Jessa24kRUS\">Chceš si se mnou zatancovat?</voice></speak>"
        //val str = "<speak version=\"1.0\" xmlns=\"http://www.w3.org/2001/10/synthesis\" xml:lang=\"en-US\"><say-as type=\"date:mdy\"> 1/29/2009 </say-as></speak>";
        //val str = "<speak>Chceš si se mnou zatancovat?</speak>"
        val str = "<speak><s>Mluv na mě jen když zrovna svítí moje zelené světlo, </s> <s>jinak bych tě bohužel neslyšela.</s> </speak>"
        val audioData = speak(TtsRequest("Milan", str, true))
        println("${audioData.size}")
        File("/Users/tomas.zajicek/Downloads/testx.mp3").writeBytes(audioData)
    }
}