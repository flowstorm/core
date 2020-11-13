package com.promethist.port.alexa.handlers

import com.promethist.port.BotService
import com.amazon.ask.dispatcher.request.handler.HandlerInput
import com.amazon.ask.model.IntentRequest
import com.amazon.ask.model.ui.PlainTextOutputSpeech
import com.amazon.ask.request.Predicates.intentName

class MessageIntentHandler : AbstractHandler(intentName("MessageIntent")) {

    class ResponseBuilder : com.amazon.ask.response.ResponseBuilder() {

        fun withText(text: String): ResponseBuilder {
            speech = PlainTextOutputSpeech.builder().withText(text).build()
            return this
        }
    }

    override fun handle(input: HandlerInput) = withContext(input) {
        val text = (input.requestEnvelope.request as IntentRequest).intent.slots["text"]!!.value
        if (text == "tell device id") {
            ResponseBuilder()
                    .withText(context.sender)
                    .withShouldEndSession(true)
        } else {
            val speech = BotService.client.doText(context, text)
            addResponse(speech)
        }
    }.build()
}