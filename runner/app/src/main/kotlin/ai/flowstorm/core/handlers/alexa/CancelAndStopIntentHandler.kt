package ai.flowstorm.core.handlers.alexa

import com.amazon.ask.dispatcher.request.handler.HandlerInput
import com.amazon.ask.request.Predicates.intentName
import ai.flowstorm.core.BotCore

class CancelAndStopIntentHandler : AmazonAlexaHandler(intentName("AMAZON.StopIntent").or(intentName("AMAZON.CancelIntent"))) {

    override fun handle(input: HandlerInput) = withContext(input) {
        val speech = BotCore.doBye(context)
        context.sessionId = null
        addResponse(speech)
    }.build()
}