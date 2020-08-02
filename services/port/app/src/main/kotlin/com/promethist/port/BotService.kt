package com.promethist.port

import ai.promethist.client.BotContext
import ai.promethist.client.BotCoreClient
import com.promethist.common.TextConsole
import com.promethist.common.RestClient
import com.promethist.common.ServiceUrlResolver
import com.promethist.core.Defaults
import com.promethist.core.Response
import com.promethist.core.resources.CoreResource
import com.promethist.core.type.Dynamic
import java.util.*
import kotlin.random.Random

object BotService {

    val coreUrl = ServiceUrlResolver.getEndpointUrl("core")
    val client = BotCoreClient(RestClient.instance(CoreResource::class.java, coreUrl))

    private val contexts = mutableMapOf<String, BotContext>()

    fun context(sessionId: String, deviceId: String, appKey: String, locale: Locale = Defaults.locale, attributes: Dynamic = Dynamic()) =
        contexts.computeIfAbsent(sessionId) {
            BotContext(coreUrl, appKey, deviceId, sessionId = sessionId, locale = locale, attributes = attributes)
        }

    @JvmStatic
    fun main(args: Array<String>) {
        val context = context("bot-" + Random.nextLong(0x10000000000000L, 0xFFFFFFFFFFFFFFL).toString(16), "test", "promethist")
        object : TextConsole() {

            init {
                addResponse(client.doIntro(context))
            }

            override fun afterInput(text: String) {
                addResponse(client.doText(context, text))
                if (context.sessionId == null)
                    stop = true
            }

            override fun done() = addResponse(client.doBye(context))

            fun addResponse(response: Response) = output.println("< [${context.sessionId}] ${response.text()}")
        }.run()
    }
}