package com.promethistai.port.bot

object BotServiceFactory {

    fun create(provider: String = "illusionist"): BotService {
        when (provider) {
            "illusionist" -> return IllusionistService()
            else -> throw NotImplementedError()
        }
    }

}