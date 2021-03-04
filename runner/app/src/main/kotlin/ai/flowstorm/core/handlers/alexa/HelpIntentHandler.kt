package ai.flowstorm.core.handlers.alexa

import com.amazon.ask.dispatcher.request.handler.HandlerInput
import com.amazon.ask.request.Predicates.intentName
import ai.flowstorm.core.Bot

class HelpIntentHandler : AmazonAlexaHandler(intentName("AMAZON.HelpIntent")) {

    override fun handle(input: HandlerInput) = withContext(input) {
        val speech = Bot.doHelp(context)
        addResponse(speech)
    }.build()
}