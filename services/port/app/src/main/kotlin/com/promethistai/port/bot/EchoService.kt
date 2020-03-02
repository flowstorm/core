package com.promethistai.port.bot

import com.promethistai.core.model.Message
import com.promethistai.core.resources.BotService

/**
 * Simple bot service responding with request text.
 */
class EchoService : BotService {

    override fun message(appKey: String, message: Message): Message = message

}