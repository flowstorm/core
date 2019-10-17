package com.promethistai.port.bot

import com.promethistai.port.DataService
import com.promethistai.port.model.Message
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

        // log incoming message
        dataService.logMessage(message)

        logger.info("message > (botService = $botService, key = $appKey) $message")

        val response = botService.message(appKey, message)

        logger.info("message < $response")

        // log response message
        if (response != null)
            dataService.logMessage(response)

        return response
    }

}