package com.promethistai.port.bot

import com.promethistai.port.DataService
import com.promethistai.port.model.Message
import org.slf4j.LoggerFactory
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

    private fun getBotService(key: String): BotService {
        val name = dataService.getContract(key).bot
        return if (name == "remote")
            remoteService
        else
            //TODO catch reflection exception
            return javaClass.getDeclaredField("${name}Service").get(this) as BotService
    }

    override fun message(appKey: String, message: Message): Message? {
        val botService = getBotService(appKey)

        // log incoming message
        dataService.logMessage(message)

        val response = botService.message(appKey, message)

        logger.info("message(botService = $botService, key = $appKey, message = $message, response = $response)")

        // log response message
        if (response != null)
            dataService.logMessage(response)

        return response
    }

}