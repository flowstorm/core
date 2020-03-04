package com.promethist.port

import com.promethist.common.AppConfig
import com.promethist.common.RestClient
import com.promethist.core.ServiceUtil
import com.promethist.core.model.Message
import com.promethist.core.resources.BotService
import org.slf4j.LoggerFactory
import javax.inject.Inject

class BotRemoteService : BotService {

    @Inject
    lateinit var appConfig: AppConfig

    @Inject
    lateinit var dataService: DataService

    private var logger = LoggerFactory.getLogger(BotRemoteService::class.java)

    override fun message(appKey: String, message: Message): Message? {
        Application.validateKey(appKey)
        var remoteEndpoint = ServiceUtil.getEndpointUrl("admin",
                ServiceUtil.RunMode.valueOf(appConfig.get("service.mode", "dist")))
        if (!remoteEndpoint.startsWith("http"))
            remoteEndpoint = ServiceUtil.getEndpointUrl(remoteEndpoint)
        val botService = RestClient.instance(BotService::class.java, remoteEndpoint)
            logger.info("remoteEndpoint = $remoteEndpoint, appKey = $appKey")
        return botService.message(appKey, message)
    }

}