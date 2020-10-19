package com.promethist.port.alexa.handlers

import com.amazon.ask.dispatcher.request.handler.HandlerInput
import com.amazon.ask.model.Response
import com.amazon.ask.model.SessionEndedRequest
import com.amazon.ask.request.Predicates.requestType
import com.promethist.port.BotService
import java.util.*

class SessionEndedRequestHandler : AbstractHandler(requestType(SessionEndedRequest::class.java)) {

    override fun handle(input: HandlerInput): Optional<Response> {
        val context = getContext(input)
        context.attributes["alexaSessionEndedReason"] = (input.requestEnvelope as SessionEndedRequest).reason.toString()
        BotService.client.doBye(context)
        return Optional.empty()
    }
}