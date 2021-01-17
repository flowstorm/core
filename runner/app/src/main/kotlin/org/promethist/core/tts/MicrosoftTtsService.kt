package org.promethist.core.tts

import org.promethist.common.AppConfig
import org.promethist.common.RestClient
import org.promethist.core.model.Voice
import org.promethist.util.LoggerDelegate
import org.w3c.dom.Document
import java.io.*
import javax.ws.rs.client.Entity
import javax.ws.rs.client.Invocation
import javax.ws.rs.core.Response
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

    private var token = ""
    private var tokenIssued = 0L
    private val logger by LoggerDelegate()

    fun call(url: String, builder: (Invocation.Builder.() -> Response)): Response {
        val target = RestClient.webTarget(url)
        val res = builder(target.request())
        if (res.status != 200) {
            logger.error(res.readEntity(String::class.java))
            error(res.statusInfo.reasonPhrase)
        }
        return res
    }

    fun synthetize(ssml: String): ByteArray {
        val now = System.currentTimeMillis()
        if (tokenIssued + 600 * 1000 < now) {
            token = call("https://${AppConfig.instance["mscs.location"]}.api.cognitive.microsoft.com/sts/v1.0/issueToken") {
                header("Ocp-Apim-Subscription-Key", AppConfig.instance["mscs.key"])
                        .post(Entity.text(""))
            }.readEntity(String::class.java)
            tokenIssued = now
        }
        call("https://${AppConfig.instance["mscs.location"]}.tts.speech.microsoft.com/cognitiveservices/v1") {
            header("Authorization", "Bearer $token")
                    .header("Content-Type", "application/ssml+xml")
                    .header("X-Microsoft-OutputFormat", "audio-16khz-64kbitrate-mono-mp3")
                    .post(Entity.text(ssml))
        }.readEntity(InputStream::class.java).use {
            val buf = ByteArrayOutputStream()
            it.copyTo(buf)
            return buf.toByteArray()
        }
    }

    override fun speak(ttsRequest: TtsRequest): ByteArray {
        // we need to transform SSML to match Microsoft requirements
        val ssml = XmlTransformer.transform(if (ttsRequest.isSsml) ttsRequest.text else "<speak>${ttsRequest.text}</speak>") { ssml ->
            val speak = ssml.documentElement
            speak.setAttribute("version", "1.0")
            speak.setAttribute("xmlns", "https://www.w3.org/2001/10/synthesis")
            speak.setAttribute("xmlns:mstts", "https://www.w3.org/2001/mstts")
            speak.setAttribute("xml:lang", ttsRequest.config.locale.toLanguageTag())
            if (speak.getElementsByTagName("voice").length == 0) {
                val voice = ssml.createElement("voice")
                voice.setAttribute("name", ttsRequest.config.name)
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
        return synthetize(ssml)
    }

    @JvmStatic
    @Throws(Exception::class)
    fun main(args: Array<String>) {
        val str = "<speak><s>Mluv na mě jen když zrovna svítí světlo.</s> <s>Je to jasné?</s> </speak>"
        val audioData = speak(TtsRequest(Voice.Milan.config, str, true))
        println("${audioData.size}")
        File("/Users/tomas.zajicek/Downloads/testx.mp3").writeBytes(audioData)
    }
}