package com.promethist.port.bot

import com.promethist.core.model.Message
import com.promethist.core.resources.BotService

/**
 * Simple bot service responding with request text.
 */
class EchoService : BotService {

    override fun message(appKey: String, message: Message): Message = message

}