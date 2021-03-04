package ai.flowstorm.core.handlers.alexa

import com.amazon.ask.dispatcher.request.handler.HandlerInput
import com.amazon.ask.model.Response
import com.amazon.ask.model.SessionEndedRequest
import com.amazon.ask.request.Predicates.requestType
import ai.flowstorm.core.Bot
import java.util.*

class SessionEndedRequestHandler : AmazonAlexaHandler(requestType(SessionEndedRequest::class.java)) {

    override fun handle(input: HandlerInput): Optional<Response> {
        val context = getContext(input)
        //context.attributes["alexaSessionEndedReason"] = (input.requestEnvelope as SessionEndedRequest).reason.toString()
        Bot.doBye(context)
        return Optional.empty()
    }
}