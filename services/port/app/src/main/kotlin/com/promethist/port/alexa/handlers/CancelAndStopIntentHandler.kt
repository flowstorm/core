package com.promethist.port.alexa.handlers

import com.promethist.port.BotService
import com.amazon.ask.dispatcher.request.handler.HandlerInput
import com.amazon.ask.request.Predicates.intentName

class CancelAndStopIntentHandler : AbstractHandler(intentName("AMAZON.StopIntent")) {

    override fun handle(input: HandlerInput) = withContext(input) {
        val speech = BotService.client.doBye(context)
        context.sessionId = null
        addResponse(speech)
    }.build()
}