package com.promethist.port.alexa.handlers

import com.amazon.ask.dispatcher.request.handler.HandlerInput
import com.amazon.ask.model.ui.PlainTextOutputSpeech
import com.amazon.ask.request.Predicates.intentName

class DeviceIdIntentHandler : AbstractHandler(intentName("DeviceIdIntent")) {

    class ResponseBuilder : com.amazon.ask.response.ResponseBuilder() {

        fun withText(text: String): ResponseBuilder {
            speech = PlainTextOutputSpeech.builder().withText(text).build()
            return this
        }
    }

    override fun handle(input: HandlerInput) = withContext(input) {
        ResponseBuilder()
                .withText(context.sender)
                .withShouldEndSession(true)
    }.build()
}