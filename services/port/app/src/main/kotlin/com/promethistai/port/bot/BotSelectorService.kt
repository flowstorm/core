package com.promethistai.port.bot

import com.promethistai.port.ConfigService
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

    private fun getBotService(key: String): BotService {
        val name = configService.getConfig(key).contract["bot"]
        return if (name == "remote")
            remoteService
        else
            //TODO catch reflection exception
            return javaClass.getDeclaredField("${name}Service").get(this) as BotService
    }

    override fun message(key: String, text: String): BotService.Response =
        getBotService(key).message(key, text)

    override fun welcome(key: String): String =
        getBotService(key).welcome(key)
}