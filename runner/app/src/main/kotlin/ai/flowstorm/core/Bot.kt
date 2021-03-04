package ai.flowstorm.core

import ai.flowstorm.client.BotContext
import ai.flowstorm.common.RestClient
import ai.flowstorm.common.ServiceUrlResolver
import ai.flowstorm.core.resources.CoreResource
import ai.flowstorm.core.type.Dynamic
import java.util.*

object Bot {

    val url = ServiceUrlResolver.getEndpointUrl("core", ServiceUrlResolver.RunMode.local)
    val resource = RestClient.resource(CoreResource::class.java, url)

    private val contexts = mutableMapOf<String, BotContext>()

    fun context(sessionId: String, deviceId: String, appKey: String, token: String? = null, locale: Locale = Defaults.locale, attributes: Dynamic = Dynamic()) =
        contexts.computeIfAbsent(sessionId) {
            BotContext(url, appKey, deviceId, token = token, sessionId = sessionId, locale = locale, attributes = attributes)
        }

    private fun createRequest(context: BotContext, text: String): Request =
            Request(context.key, context.deviceId, context.token, context.sessionId!!, context.initiationId,
                    Input(context.locale, context.zoneId, Input.Transcript(text)), context.attributes)

    private fun doRequest(request: Request) = resource.process(request)

    fun doIntro(context: BotContext) = doText(context, context.introText)

    fun doText(context: BotContext, text: String) = doRequest(createRequest(context, text))

    fun doHelp(context: BotContext) = doRequest(createRequest(context, "help"))

    fun doBye(context: BotContext) = doRequest(createRequest(context, "stop"))
}