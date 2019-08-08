package com.promethistai.port.bot

import com.promethistai.common.RestClient
import com.promethistai.port.ConfigService
import javax.inject.Inject

class BotRemoteService : BotService {

    @Inject
    lateinit var configService: ConfigService

    private fun getBotService(key: String): BotService {
        val remoteEndpoint = configService.getConfig(key).contract["remoteEndpoint"] as String
        return RestClient.instance<BotService>(BotService::class.java, remoteEndpoint)
    }

    override fun process(key: String, text: String): BotService.Response =
            getBotService(key).process(key, text)

    override fun welcome(key: String): String =
            getBotService(key).welcome(key)

}