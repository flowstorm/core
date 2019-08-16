package com.promethistai.port.bot

import com.promethistai.port.ConfigService
import org.slf4j.LoggerFactory
import javax.inject.Inject

class BotSelectorService : BotService {

    @Inject
    lateinit var configService: ConfigService

    @Inject
    lateinit var remoteService: BotRemoteService

    @Inject
    lateinit var echoService: EchoService

    @Inject
    lateinit var illusionistService: IllusionistService

    private var logger = LoggerFactory.getLogger(BotSelectorService::class.java)

    private fun getBotService(key: String): BotService {
        val name = configService.getConfig(key).contract["bot"]
        return if (name == "remote")
            remoteService
        else
            //TODO catch reflection exception
            return javaClass.getDeclaredField("${name}Service").get(this) as BotService
    }

    override fun message(key: String, message: Message): Message? {
        val response = getBotService(key).message(key, message)
        if (logger.isInfoEnabled)
            logger.info("message = $message, response = $response")
        return response
    }

}