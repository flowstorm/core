package ai.flowstorm.core.handlers.alexa

import com.amazon.ask.dispatcher.request.handler.HandlerInput
import com.amazon.ask.model.IntentRequest
import com.amazon.ask.request.Predicates.intentName
import ai.flowstorm.core.BotCore

class MessageIntentHandler : AmazonAlexaHandler(intentName("MessageIntent")) {

    override fun handle(input: HandlerInput) = withContext(input) {
        val text = (input.requestEnvelope.request as IntentRequest).intent.slots["text"]!!.value
        if (text == "tell device id") {
            input.attributesManager.sessionAttributes["deviceId"] = context.deviceId
            input.responseBuilder
                    .withSpeech("")
                    .withShouldEndSession(true)
        } else {
            val speech = BotCore.doText(context, text)
            addResponse(speech)
        }
    }.build()
}