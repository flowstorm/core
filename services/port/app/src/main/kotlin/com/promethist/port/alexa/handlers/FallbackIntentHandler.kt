package com.promethist.port.alexa.handlers

import com.promethist.port.BotService
import com.amazon.ask.dispatcher.request.handler.HandlerInput
import com.amazon.ask.request.Predicates.intentName

class FallbackIntentHandler : AbstractHandler(intentName("AMAZON.FallbackIntent")) {

    override fun handle(input: HandlerInput) = withContext(input) {
        val speech = BotService.client.doHelp(context)
        addResponse(speech)
    }.build()
}