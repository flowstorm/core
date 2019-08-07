package com.promethistai.port.bot

import com.google.gson.GsonBuilder
import com.promethistai.common.AppConfig
import com.promethistai.port.PortResource
import java.io.IOException
import java.io.InputStreamReader
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.net.ssl.HttpsURLConnection

class IllusionistService : BotService {

    //var portResource: PortResource? = null @Inject set
    @Inject lateinit var appConfig: AppConfig

    override fun process(text: String): BotService.Response {
        try {
            val url = URL("""https://illusionist.${appConfig["namespace"]}.promethist.ai/query/GlobalRepeat1?key=AIzaSyDgHsjHyK4cS11nEUJuRGeVUEDITi6OtZA&query=${URLEncoder.encode(text, "utf-8")}""")
            val responses = restCall(url, Array<BotService.Response>::class.java, "POST")
            return if (responses.isNotEmpty())
                responses[0]
            else
                BotService.Response("?", 0.0)
        } catch (e: IOException) {
            e.printStackTrace()
            return BotService.Response("Error: $e", 1.0)
        }
    }

    fun <T>restCall(url: URL, responseType: Class<T>, method: String = "GET", output: Any? = null): T {
        val conn = url.openConnection() as HttpsURLConnection
        conn.readTimeout = 10000
        conn.connectTimeout = 15000
        conn.requestMethod = method
        conn.doInput = true
        conn.doOutput = false //TODO output object
        conn.connect()
        return GsonBuilder().create().fromJson<T>(InputStreamReader(conn.inputStream), responseType)
    }

    override fun welcome(): String {
        return "Hi!"
    }

}