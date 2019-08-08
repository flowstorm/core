package com.promethistai.port.bot

import java.io.Serializable

interface BotService {

    data class Response(
        var answer: String,
        var confidence: Double) : Serializable

    fun process(key: String, text: String): Response

    fun welcome(key: String): String

}
