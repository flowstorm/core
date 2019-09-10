package com.promethistai.port.bot

import com.promethistai.common.RestClient
import com.promethistai.port.DataService
import com.promethistai.port.model.Message
import org.slf4j.LoggerFactory
import javax.inject.Inject

class BotRemoteService : BotService {

    @Inject
    lateinit var dataService: DataService

    private var logger = LoggerFactory.getLogger(BotRemoteService::class.java)

    override fun message(key: String, message: Message): Message? {
        val contract = dataService.getContract(key)
        val remoteEndpoint = contract.remoteEndpoint!!
        val botService = RestClient.instance<BotService>(BotService::class.java, remoteEndpoint)
        val botKey = contract.botKey?:key
        val model: String? = message.recipient ?: contract.model
        if (logger.isInfoEnabled)
            logger.info("remoteEndpoint = $remoteEndpoint, botKey = $botKey, model = $model")
        return botService.message(botKey, message.apply { this.recipient = model })
    }

}