package com.promethistai.port.bot

import com.promethistai.common.RestClient
import com.promethistai.port.ConfigService
import org.slf4j.LoggerFactory
import javax.inject.Inject

class BotRemoteService : BotService {

    @Inject
    lateinit var configService: ConfigService

    private var logger = LoggerFactory.getLogger(BotRemoteService::class.java)

    override fun message(key: String, message: Message): Message? {
        val contract = configService.getConfig(key).contract
        val remoteEndpoint = contract.remoteEndpoint!!
        val botService = RestClient.instance<BotService>(BotService::class.java, remoteEndpoint)
        val botKey = contract.botKey?:key
        if (logger.isInfoEnabled)
            logger.info("remoteEndpoint = $remoteEndpoint, botKey = $botKey")
        return botService.message(botKey, message)
    }

}