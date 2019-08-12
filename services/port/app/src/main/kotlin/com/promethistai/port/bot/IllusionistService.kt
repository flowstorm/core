package com.promethistai.port.bot

import com.promethistai.common.AppConfig
import com.promethistai.common.RestClient
import com.promethistai.port.ConfigService
import java.io.IOException
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject

class IllusionistService : BotService {

    @Inject
    lateinit var appConfig: AppConfig

    @Inject
    lateinit var configService: ConfigService

    override fun message(key: String, text: String): BotService.Response {
        try {
            val contract = configService.getConfig(key).contract
            val botKey = contract["botKey"]?:key
            val model = contract["model"]?:"GlobalRepeat1"
            val url = URL("""https://illusionist.${appConfig["namespace"]}.promethist.ai/query/${model}?key=${botKey}&query=${URLEncoder.encode(text, "utf-8")}""")
            val responses = RestClient.call(url, Array<BotService.Response>::class.java, "POST")
            return if (responses.isNotEmpty())
                responses[0]
            else
                BotService.Response("?", 0.0)
        } catch (e: IOException) {
            e.printStackTrace()
            return BotService.Response("Error: $e", 1.0)
        }
    }

    override fun welcome(key: String): String {
        return "Hi!"
    }

}