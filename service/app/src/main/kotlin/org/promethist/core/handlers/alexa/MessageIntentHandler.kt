package org.promethist.core.handlers.alexa

import org.promethist.core.BotCore
import com.amazon.ask.dispatcher.request.handler.HandlerInput
import com.amazon.ask.model.IntentRequest
import com.amazon.ask.request.Predicates.intentName

class MessageIntentHandler : AbstractHandler(intentName("MessageIntent")) {

    override fun handle(input: HandlerInput) = withContext(input) {
        val text = (input.requestEnvelope.request as IntentRequest).intent.slots["text"]!!.value
        if (text == "tell device id") {
            input.attributesManager.sessionAttributes["deviceId"] = context.sender
            input.responseBuilder
                    .withSpeech("")
                    .withShouldEndSession(true)
        } else {
            val speech = BotCore.doText(context, text)
            addResponse(speech)
        }
    }.build()
}