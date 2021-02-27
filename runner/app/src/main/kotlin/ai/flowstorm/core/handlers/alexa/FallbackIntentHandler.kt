package ai.flowstorm.core.handlers.alexa

import com.amazon.ask.dispatcher.request.handler.HandlerInput
import com.amazon.ask.request.Predicates.intentName
import ai.flowstorm.core.BotCore

class FallbackIntentHandler : AmazonAlexaHandler(intentName("AMAZON.FallbackIntent")) {

    override fun handle(input: HandlerInput) = withContext(input) {
        val speech = BotCore.doHelp(context)
        addResponse(speech)
    }.build()
}