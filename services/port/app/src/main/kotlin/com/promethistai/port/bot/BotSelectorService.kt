package com.promethistai.port.bot

import com.promethistai.core.model.Message
import com.promethistai.core.resources.BotService
import com.promethistai.port.DataService
import org.slf4j.LoggerFactory
import java.util.*
import javax.inject.Inject

class BotSelectorService : BotService {

    @Inject
    lateinit var dataService: DataService

    @Inject
    lateinit var remoteService: BotRemoteService

    @Inject
    lateinit var echoService: EchoService

    @Inject
    lateinit var illusionistService: IllusionistService

    private var logger = LoggerFactory.getLogger(BotSelectorService::class.qualifiedName)

    override fun message(appKey: String, message: Message): Message? {
        val contract = dataService.getContract(appKey)
        val botService = if (contract.bot == "remote")
            remoteService
        else
            //TODO catch reflection exception
            javaClass.getDeclaredField("${contract.bot}Service").get(this) as BotService

        if (message.language == null)
            message.language = Locale(contract.language)

        logger.info("message > (botService = $botService, key = $appKey) $message")

        val currentTime = System.currentTimeMillis()
        val response = botService.message(appKey, message)
        if (response != null)
            response!!.extensions["serviceResponseTime"] = System.currentTimeMillis() - currentTime

        logger.info("message < $response")

        return response
    }

}