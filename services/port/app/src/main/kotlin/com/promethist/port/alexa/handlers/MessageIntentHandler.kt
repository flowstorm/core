package com.promethist.port.alexa.handlers

import com.promethist.port.BotService
import com.amazon.ask.dispatcher.request.handler.HandlerInput
import com.amazon.ask.model.IntentRequest
import com.amazon.ask.request.Predicates.intentName

class MessageIntentHandler : AbstractHandler(intentName("MessageIntent")) {

    override fun handle(input: HandlerInput) = withContext(input) {
        val text = (input.requestEnvelope.request as IntentRequest).intent.slots["text"]!!.value
        val speech = BotService.client.doText(context, text)
        addResponse(speech)
    }.build()
}