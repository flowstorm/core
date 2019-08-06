package com.promethistai.port.bot

import com.google.gson.GsonBuilder
import com.promethistai.common.AppConfig
import java.io.IOException
import java.io.InputStreamReader
import java.net.URL
import java.net.URLEncoder
import javax.net.ssl.HttpsURLConnection

class IllusionistService : BotService {

    override fun process(text: String): BotService.Response {
        try {
            val url = URL("""https://illusionist.${AppConfig.instance["namespace"]}.promethist.ai/query/GlobalRepeat1?key=AIzaSyDgHsjHyK4cS11nEUJuRGeVUEDITi6OtZA&query=${URLEncoder.encode(text, "utf-8")}""")

            val conn = url.openConnection() as HttpsURLConnection
            conn.readTimeout = 10000
            conn.connectTimeout = 15000
            conn.requestMethod = "POST"
            conn.doInput = true
            conn.doOutput = false
            conn.connect()

            val responses = GsonBuilder().create().fromJson<Array<BotService.Response>>(InputStreamReader(conn.inputStream), Array<BotService.Response>::class.java)
            return if (responses.isNotEmpty())
                responses[0]
            else
                BotService.Response("?", 0.0)
        } catch (e: IOException) {
            e.printStackTrace()
            return BotService.Response("Error: $e", 1.0)
        }
    }

    override fun welcome(): String {
        return "Hi!"
    }

}