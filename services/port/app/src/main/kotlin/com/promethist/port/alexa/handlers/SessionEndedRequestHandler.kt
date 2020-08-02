package com.promethist.port.alexa.handlers

import com.amazon.ask.dispatcher.request.handler.HandlerInput
import com.amazon.ask.model.Response
import com.amazon.ask.model.SessionEndedRequest
import com.amazon.ask.request.Predicates.requestType
import java.util.*

class SessionEndedRequestHandler : AbstractHandler(requestType(SessionEndedRequest::class.java)) {

    override fun handle(input: HandlerInput): Optional<Response> {
        logger.info("${this::class.simpleName}.handle")
        // any cleanup logic goes here
        return input.responseBuilder.build()
    }
}