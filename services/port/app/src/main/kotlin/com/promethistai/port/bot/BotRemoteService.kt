package com.promethistai.port.bot

import com.promethistai.common.RestClient
import com.promethistai.port.ConfigService
import javax.inject.Inject

class BotRemoteService : BotService {

    class ServiceRef(val service: BotService, val key: String)

    @Inject
    lateinit var configService: ConfigService

    private fun getServiceRef(key: String): ServiceRef {
        val contract = configService.getConfig(key).contract
        return ServiceRef(
                RestClient.instance<BotService>(BotService::class.java, contract["remoteEndpoint"] as String),
                (contract["botKey"] as String)?:key)
    }

    override fun message(key: String, text: String): BotService.Response {
        val ref = getServiceRef(key)
        return ref.service.message(ref.key, text)
    }

    override fun welcome(key: String): String {
        val ref = getServiceRef(key)
        return ref.service.welcome(ref.key)
    }

}