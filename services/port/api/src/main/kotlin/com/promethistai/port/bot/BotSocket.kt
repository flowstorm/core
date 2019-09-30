package com.promethistai.port.bot

interface BotSocket {

    fun onEvent(event: BotEvent)

    fun sendEvent(event: BotEvent)
}