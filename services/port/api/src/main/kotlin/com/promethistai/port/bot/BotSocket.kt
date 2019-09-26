package com.promethistai.port.bot

import java.io.IOException

interface BotSocket {

    fun onEvent(event: BotEvent)

    fun sendEvent(event: BotEvent)
}