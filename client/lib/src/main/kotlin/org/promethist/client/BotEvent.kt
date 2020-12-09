package org.promethist.client

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
        JsonSubTypes.Type(value = BotEvent.Init::class, name = "Init"),
        JsonSubTypes.Type(value = BotEvent.Ready::class, name = "Ready"),
        JsonSubTypes.Type(value = BotEvent.Error::class, name = "Error"),
        JsonSubTypes.Type(value = BotEvent.Request::class, name = "Request"),
        JsonSubTypes.Type(value = BotEvent.Response::class, name = "Response"),
        JsonSubTypes.Type(value = BotEvent.Recognized::class, name = "Recognized"),
        JsonSubTypes.Type(value = BotEvent.SessionStarted::class, name = "SessionStarted"),
        JsonSubTypes.Type(value = BotEvent.SessionEnded::class, name = "SessionEnded"),
        JsonSubTypes.Type(value = BotEvent.InputAudioStreamOpen::class, name = "InputAudioStreamOpen"),
        JsonSubTypes.Type(value = BotEvent.InputAudioStreamClose::class, name = "InputAudioStreamClose")
)
open class BotEvent {
    data class Init(val key: String, @JsonAlias("sender") val deviceId: String, val token: String? = null, val config: BotConfig) : BotEvent()
    class Ready : BotEvent()
    data class Error(val text: String) : BotEvent()
    data class Request(val request: org.promethist.core.Request) : BotEvent()
    data class Response(val response: org.promethist.core.Response) : BotEvent()
    data class Recognized(val text: String) : BotEvent()
    data class SessionStarted(val sessionId: String) : BotEvent()
    class SessionEnded : BotEvent()
    class InputAudioStreamOpen : BotEvent()
    class InputAudioStreamClose : BotEvent()

    override fun toString(): String = if (this::class.isData) super.toString() else this::class.simpleName!!
}
