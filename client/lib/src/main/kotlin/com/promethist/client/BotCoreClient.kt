package com.promethist.client

import com.promethist.client.BotContext
import com.promethist.core.Input
import com.promethist.core.Request
import com.promethist.core.resources.CoreResource
import com.promethist.util.LoggerDelegate
import java.util.*

/**
 * BotCoreClient represents bot client implementation connected directly to Core resource. It is stateless
 * (every method requires BotContext) and supports only text communication (used by Alexa/Google assistant apps).
 */
open class BotCoreClient(private val coreResource: CoreResource) {

    val logger by LoggerDelegate()

    private fun createRequest(context: BotContext, text: String): Request =
            Request(context.key, context.sender, context.token, context.sessionId!!,
                    Input(context.locale, context.zoneId, Input.Transcript(text)), context.attributes)

    fun doRequest(context: BotContext, request: Request) = coreResource.process(request)

    fun doIntro(context: BotContext) = doText(context, context.introText)

    fun doText(context: BotContext, text: String) = doRequest(context, createRequest(context, text))

    fun doHelp(context: BotContext) = doRequest(context, createRequest(context, "help"))

    fun doBye(context: BotContext) = doRequest(context, createRequest(context, "stop"))

    fun getString(locale: Locale, key: String): String = ResourceBundle.getBundle("resources", Locale(locale.language)).getString(key)
}
