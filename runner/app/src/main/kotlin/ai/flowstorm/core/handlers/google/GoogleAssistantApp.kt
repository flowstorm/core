package ai.flowstorm.core.handlers.google

import com.google.actions.api.ActionRequest
import com.google.actions.api.ActionsSdkApp
import com.google.actions.api.ForIntent
import com.google.actions.api.response.ResponseBuilder
import com.google.api.services.actions_fulfillment.v2.model.BasicCard
import com.google.api.services.actions_fulfillment.v2.model.Image
import com.google.api.services.actions_fulfillment.v2.model.SimpleResponse
import ai.flowstorm.client.BotContext
import ai.flowstorm.common.AppConfig
import ai.flowstorm.core.Bot
import ai.flowstorm.core.Response
import ai.flowstorm.core.type.Dynamic
import ai.flowstorm.util.LoggerDelegate
import java.util.*

class GoogleAssistantApp : ActionsSdkApp() {

    companion object {
        val appKey = object : ThreadLocal<String>() {
            override fun initialValue() = "default"
        }
    }

    val appTitle = AppConfig.instance["title"]

    private val logger by LoggerDelegate()

    inner class ContextualBlock(val request: ActionRequest, val context: BotContext) {
        fun addResponse(response: Response) = getResponseBuilder(request).apply {
            add(SimpleResponse().apply {
                displayText = response.text()
                ssml = response.ssml(Response.IVA.GoogleAssistant)
            })
            response.items.forEach {
                if (it.image != null) {
                    add(BasicCard().apply {
                        title = appTitle
                        if (!it.text.isNullOrBlank())
                            subtitle = it.text
                        image = Image().apply {
                            url = it.image
                            accessibilityText = "(alt)"
                        }
                    })
                }
            }
            context.sessionId ?: endConversation()
        }
    }

    private fun withContext(request: ActionRequest, block: ContextualBlock.() -> ResponseBuilder): ResponseBuilder {
        val context = Bot.context(
                request.sessionId!!,
                "google-device",
                appKey.get(),
                null,
                Locale.ENGLISH/*, request.appRequest?.user?.locale?*/,
                Dynamic(
                        "clientType" to "google-assistant:${AppConfig.version}"
                )
        )
        logger.info("${this::class.simpleName}.withContext(request=$request, context=$context)")
        return block(ContextualBlock(request, context))
    }


    @ForIntent("actions.intent.MAIN")
    fun onMainIntent(request: ActionRequest) = withContext(request) {
        val response = Bot.doIntro(context)
        addResponse(response)
    }.build()

    @ForIntent("actions.intent.TEXT")
    fun onTextIntent(request: ActionRequest) = withContext(request) {
        val text = request.getArgument("text")!!.textValue
        val response = Bot.doText(context, text)
        addResponse(response)
    }.build()

}