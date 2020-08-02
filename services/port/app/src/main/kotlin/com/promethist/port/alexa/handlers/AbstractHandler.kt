package com.promethist.port.alexa.handlers

import com.promethist.port.BotService
import ai.promethist.client.BotContext
import com.amazon.ask.dispatcher.request.handler.HandlerInput
import com.amazon.ask.dispatcher.request.handler.RequestHandler
import com.amazon.ask.model.interfaces.display.BodyTemplate7
import com.amazon.ask.model.interfaces.display.Image
import com.amazon.ask.model.interfaces.display.ImageInstance
import com.amazon.ask.model.interfaces.display.RenderTemplateDirective
import com.amazon.ask.response.ResponseBuilder
import com.promethist.common.AppConfig
import com.promethist.core.Response
import com.promethist.core.model.TtsConfig
import com.promethist.core.type.Dynamic
import com.promethist.util.LoggerDelegate
import java.util.*
import java.util.function.Predicate

abstract class AbstractHandler(private val predicate: Predicate<HandlerInput>) : RequestHandler {

    val title = AppConfig.instance["title"]

    inner class ContextualBlock(val input: HandlerInput, val context: BotContext) {

        fun hasDisplayInterface() = input.requestEnvelope.context.system.device.supportedInterfaces.display != null
        fun hasVideoApp() = input.requestEnvelope.context.system.device.supportedInterfaces.videoApp != null

        fun addResponse(response: Response): ResponseBuilder =
                input.responseBuilder.apply {
                    val shouldEndSession = response.sessionEnded
                    val ssml = response.ssml(TtsConfig.Provider.Amazon)
                    withSpeech(ssml)
                    response.items.forEach { item ->
                        // image
                        if (item.image != null) {
                            if (hasDisplayInterface()) {
                                val imageInstance = ImageInstance.builder().withUrl(item.image).build()
                                val image = Image.builder().withSources(listOf(imageInstance)).build()
                                addDirective(RenderTemplateDirective.builder()
                                        .withTemplate(BodyTemplate7.builder()
                                                .withTitle(title)
                                                .withImage(image)
                                                .build())
                                        .build())
                            } else {
                                withStandardCard(title, "", com.amazon.ask.model.ui.Image.builder()
                                        .withLargeImageUrl(item.image)
                                        .build())
                            }
                        }

                        // video
                        if (item.video != null) {
                            if (hasVideoApp())
                                addVideoAppLaunchDirective(item.video, "(title)", "(subtitle)")
                        }
                    }
                    withShouldEndSession(shouldEndSession)
                    logger.info("response = $response, shouldEndSession = $shouldEndSession")
                }

    }

    protected val logger by LoggerDelegate()

    protected fun withContext(input: HandlerInput, block: ContextualBlock.() -> ResponseBuilder): ResponseBuilder {
        val context = with (input.requestEnvelope) {
            BotService.context(session.sessionId, context.system.device.deviceId, "alexa:${context.system.application.applicationId}", Locale.ENGLISH, Dynamic(
                    "clientType" to "amazon-alexa:${AppConfig.version}"
            ))
        }
        logger.info("${this::class.simpleName}.withContext(input = $input, context = $context)")
        return block(ContextualBlock(input, context))
    }

    override fun canHandle(input: HandlerInput) = input.matches(predicate)
}