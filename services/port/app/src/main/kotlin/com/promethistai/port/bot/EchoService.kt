package com.promethistai.port.bot

import com.promethistai.port.model.Message

/**
 * Simple bot service responding with request text.
 */
class EchoService : BotService {

    override fun message(appKey: String, message: Message): Message = message

}