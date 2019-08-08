package com.promethistai.port.bot

import com.promethistai.port.bot.BotService

/**
 * Simple bot service responding with request text.
 */
class EchoService : BotService {

    override fun process(key: String, text: String): BotService.Response = BotService.Response(text, 1.0)

    override fun welcome(key: String): String = "echo"

}