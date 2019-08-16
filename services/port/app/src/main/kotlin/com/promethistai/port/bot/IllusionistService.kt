package com.promethistai.port.bot

import com.promethistai.common.AppConfig
import com.promethistai.common.RestClient
import com.promethistai.port.ConfigService
import java.io.IOException
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject

class IllusionistService : BotService {

    data class Response(
            var answer: String = "",
            var confidence: Double = 1.0)

    @Inject
    lateinit var appConfig: AppConfig

    @Inject
    lateinit var configService: ConfigService

    override fun message(key: String, message: Message): Message? {
        try {
            val contract = configService.getConfig(key).contract
            val botKey = contract["botKey"]?:key
            val model = contract["model"]?:"GlobalRepeat1"
            val url = URL("""https://illusionist.${appConfig["namespace"]}.promethist.ai/query/${model}?key=${botKey}&query=${URLEncoder.encode(message.text, "utf-8")}""")
            val responses = RestClient.call(url, Array<Response>::class.java, "POST")
            if (responses.isNotEmpty())
                return Message(responses[0].answer, mapOf("confidence" to responses[0].confidence))
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

}