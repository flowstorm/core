package com.promethistai.port.bot

import com.promethistai.common.AppConfig
import com.promethistai.common.RestClient
import com.promethistai.core.ServiceUtil
import com.promethistai.core.model.Message
import com.promethistai.core.resources.BotService
import com.promethistai.port.DataService
import org.slf4j.LoggerFactory
import javax.inject.Inject

class BotRemoteService : BotService {

    @Inject
    lateinit var appConfig: AppConfig

    @Inject
    lateinit var dataService: DataService

    private var logger = LoggerFactory.getLogger(BotRemoteService::class.java)

    override fun message(appKey: String, message: Message): Message? {
        val contract = dataService.getContract(appKey)
        var remoteEndpoint = contract.remoteEndpoint!!
        if (!remoteEndpoint.startsWith("http"))
            remoteEndpoint = ServiceUtil.getEndpointUrl(remoteEndpoint)
        val botService = RestClient.instance<BotService>(BotService::class.java, remoteEndpoint)
        val botKey = contract.botKey?:appKey
        val model: String? = contract.model?:message.recipient
        if (logger.isInfoEnabled)
            logger.info("remoteEndpoint = $remoteEndpoint, botKey = $botKey, model = $model")
        return botService.message(botKey, message.apply { this.recipient = model })
    }

}