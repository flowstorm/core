package com.promethist.port.bot

import com.promethist.common.AppConfig
import com.promethist.common.RestClient
import com.promethist.core.model.Message
import com.promethist.core.model.MessageItem
import com.promethist.core.resources.BotService
import com.promethist.port.DataService
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject

class IllusionistService : BotService {

    data class Response(var answer: String = "", var confidence: Double = 1.0)

    @Inject
    lateinit var appConfig: AppConfig

    @Inject
    lateinit var dataService: DataService

    private var logger = LoggerFactory.getLogger(IllusionistService::class.java)

    override fun message(appKey: String, message: Message): Message? {
        try {
            val contract = dataService.getContract(appKey)
            val botKey = contract.botKey?:appKey
            val model = contract.model?:"GlobalRepeat1"
            val stage = if (appConfig["namespace"] != "default") ".${appConfig["namespace"]}" else ""
            val remoteEndpoint = "https://illusionist${stage}.promethist.ai/query"
            val url = URL("""${remoteEndpoint}/${model}?key=${botKey}&query=${URLEncoder.encode(message.items.getOrNull(0)?.text?:"", "utf-8")}""")
            if (logger.isInfoEnabled)
                logger.info("remoteEndpoint = $remoteEndpoint, botKey = $botKey, model = $model")
            val responses = RestClient.call(url, Array<Response>::class.java, "POST")
            if (responses.isNotEmpty())
                return message.response(mutableListOf(MessageItem(text = responses[0].answer, confidence = responses[0].confidence)))
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

}