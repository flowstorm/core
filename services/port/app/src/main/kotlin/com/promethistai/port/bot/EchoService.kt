package com.promethistai.port.bot

import com.promethistai.port.bot.BotService

/**
 * Simple bot service responding with request text.
 */
class EchoService : BotService {

    override fun message(key: String, message: Message): Message = message

}