package com.promethist.core.handlers.alexa

import com.promethist.core.BotService
import com.amazon.ask.dispatcher.request.handler.HandlerInput
import com.amazon.ask.request.Predicates.intentName

class HelpIntentHandler : AbstractHandler(intentName("AMAZON.HelpIntent")) {

    override fun handle(input: HandlerInput) = withContext(input) {
        val speech = BotService.client.doHelp(context)
        addResponse(speech)
    }.build()
}