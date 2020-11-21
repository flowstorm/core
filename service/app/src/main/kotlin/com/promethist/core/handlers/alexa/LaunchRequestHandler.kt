package com.promethist.core.handlers.alexa

import com.promethist.core.BotService
import com.amazon.ask.dispatcher.request.handler.HandlerInput
import com.amazon.ask.model.LaunchRequest
import com.amazon.ask.model.Response
import com.amazon.ask.request.Predicates.requestType
import java.util.*

class LaunchRequestHandler : AbstractHandler(requestType(LaunchRequest::class.java)) {

    override fun handle(input: HandlerInput): Optional<Response> = withContext(input) {
        val speech = BotService.client.doIntro(context)
        addResponse(speech)
    }.build()
}