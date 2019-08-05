package com.promethistai.port.bot

import com.promethistai.port.bot.impl.IllusionistService
import java.io.Serializable

interface BotService {

    data class Response(
        var answer: String,
        var confidence: Double) : Serializable

    fun process(text: String): Response

    fun welcome(): String

    companion object {

        fun create(provider: String = "illusionist"): BotService {
            when (provider) {
                "illusionist" -> return IllusionistService()
                else -> throw NotImplementedError()
            }
        }
    }

}
