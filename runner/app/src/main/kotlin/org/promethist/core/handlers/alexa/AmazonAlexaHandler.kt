package org.promethist.core.handlers.alexa

import com.amazon.ask.dispatcher.request.handler.HandlerInput
import com.amazon.ask.dispatcher.request.handler.RequestHandler
import com.amazon.ask.model.interfaces.alexa.presentation.apl.RenderDocumentDirective
import com.amazon.ask.model.interfaces.display.BodyTemplate7
import com.amazon.ask.model.interfaces.display.Image
import com.amazon.ask.model.interfaces.display.ImageInstance
import com.amazon.ask.model.interfaces.display.RenderTemplateDirective
import com.amazon.ask.request.RequestHelper
import com.amazon.ask.response.ResponseBuilder
import com.fasterxml.jackson.core.type.TypeReference
import com.mongodb.ConnectionString
import com.mongodb.client.MongoDatabase
import org.litote.kmongo.KMongo
import org.litote.kmongo.eq
import org.litote.kmongo.getCollection
import org.promethist.client.BotContext
import org.promethist.common.AppConfig
import org.promethist.common.JerseyApplication
import org.promethist.common.ObjectUtil.defaultMapper
import org.promethist.common.monitoring.Monitor
import org.promethist.core.BotCore
import org.promethist.core.Response
import org.promethist.core.model.Card
import org.promethist.core.type.Dynamic
import org.promethist.core.type.Location
import org.promethist.util.LoggerDelegate
import java.util.*
import java.util.function.Predicate

abstract class AmazonAlexaHandler(private val predicate: Predicate<HandlerInput>) : RequestHandler {

    val title = AppConfig.instance["title"]

    private val monitor: Monitor = JerseyApplication.instance.injectionManager.getInstance(Monitor::class.java)
    private val documentJsonTemplate = """
      {
            "type": "APL",
            "version": "1.0",
            "theme": "dark",
            "import": [
                {
                  "name": "alexa-layouts",
                  "version": "1.0.0"
                }
            ],
            "resources": [],
            "styles": {},
            "layouts": {},
            "mainTemplate": {
                "parameters": [
                    "payload"
                ],
                "items": [
                    {
                        "type": "Container",
                        "width": "100%",
                        "height": "100%",
                        "items": [
                            {
                                "type": "Image",
                                "width": "100%",
                                "height": "100%",
                                "source": "${'$'}{payload.imageData.url}",
                                "align": "center"
                            }
                        ],
                        "alignItems": "center",
                        "direction": "columnReverse"
                    }
                ]
            }
        }
    """.trimIndent()

    private fun dataSourcesJson(imageUrl: String) = """
        {
          "imageData": {
            "url": "$imageUrl"
          }
        }

    """.trimIndent()

    inner class ContextualBlock(val input: HandlerInput, val context: BotContext) {

        private val hasDisplayInterface get() = input.requestEnvelope.context.system.device.supportedInterfaces.display != null
        private val hasVideoApp get() = input.requestEnvelope.context.system.device.supportedInterfaces.videoApp != null

        fun addResponse(response: Response): ResponseBuilder = input.responseBuilder.apply {
            val shouldEndSession = response.sessionEnded && response.sleepTimeout == 0
            val ssml = response.ssml(Response.IVA.AmazonAlexa)
            withSpeech(ssml)
            response.items.forEach { item ->
                // command
                if (item.text?.startsWith('#') == true) {
                    when (item.text()) {
                        "#card" -> {
                            val card = defaultMapper.readValue(item.code, Card::class.java)
                            withSimpleCard(card.title, card.text)
                        }
                    }
                }
                // image
                if (item.image != null) {
                    when {
                        RequestHelper.forHandlerInput(input).supportedInterfaces.alexaPresentationAPL != null -> {
                            val documentMapType: TypeReference<HashMap<String, Any>> =
                                object : TypeReference<HashMap<String, Any>>() {}
                            val document: Map<String, Any> =
                                defaultMapper.readValue(documentJsonTemplate, documentMapType)
                            val dataSources: Map<String, Any> = defaultMapper.readValue(
                                dataSourcesJson(item.image ?: ""),
                                documentMapType
                            )
                            val documentDirective: RenderDocumentDirective = RenderDocumentDirective.builder()
                                .withToken("imageRenderToken")
                                .withDocument(document)
                                .withDatasources(dataSources)
                                .build()
                            addDirective(documentDirective)
                        }
                        hasDisplayInterface -> {
                            val imageInstance = ImageInstance.builder().withUrl(item.image).build()
                            val image = Image.builder().withSources(listOf(imageInstance)).build()
                            addDirective(RenderTemplateDirective.builder()
                                .withTemplate(BodyTemplate7.builder()
                                    .withTitle(title)
                                    .withImage(image)
                                    .build())
                                .build())
                        }
                        else -> {
                            withStandardCard(title, "", com.amazon.ask.model.ui.Image.builder()
                                .withLargeImageUrl(item.image)
                                .build())
                        }
                    }
                }

                // video
                if (item.video != null) {
                    if (hasVideoApp)
                        addVideoAppLaunchDirective(item.video, "(title)", "(subtitle)")
                }
            }
            withShouldEndSession(shouldEndSession)
            logger.info("Adding response $response (shouldEndSession=$shouldEndSession)")
        }
    }

    protected val logger by LoggerDelegate()

    protected fun getContext(input: HandlerInput) = with (input.requestEnvelope) {
        BotCore.context(
            session.sessionId,
            context.system.device.deviceId.let { deviceId ->
                getDeviceIdMapping(deviceId)?.let { mapping ->
                    logger.info("Alexa device ID mapping from ${mapping.from} to ${mapping.to} (description=${mapping.description})")
                    mapping.to
                } ?: deviceId
            },
            "alexa:${context.system.application.applicationId}",
            "alexa:${context.system.apiAccessToken}",
            Locale.ENGLISH,
            Dynamic("clientType" to "amazon-alexa:${AppConfig.version}"
        ).also { attributes ->
            try {
                context.geolocation?.apply {
                    val location = Location(
                            coordinate?.latitudeInDegrees,
                            coordinate?.longitudeInDegrees,
                            coordinate?.accuracyInMeters,
                            altitude?.altitudeInMeters,
                            altitude?.accuracyInMeters,
                            speed?.speedInMetersPerSecond,
                            speed?.accuracyInMetersPerSecond,
                            heading?.directionInDegrees,
                            heading?.accuracyInDegrees
                    )
                    attributes["clientLocation"] = location.toString()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                monitor.capture(e)
            }
        })
    }

    protected fun withContext(input: HandlerInput, block: ContextualBlock.() -> ResponseBuilder): ResponseBuilder {
        val context = getContext(input)
        logger.info("${this::class.simpleName}.withContext(input=$input, context=$context)")
        return block(ContextualBlock(input, context))
    }

    override fun canHandle(input: HandlerInput) = input.matches(predicate)

    data class DeviceIdMapping(val from: String, val to: String, val description: String)

    companion object {

        val database: MongoDatabase by lazy {
            KMongo.createClient(ConnectionString(AppConfig.instance["database.url"]))
                .getDatabase(AppConfig.instance["name"] + "-" + AppConfig.instance.get("dsuffix", AppConfig.instance["namespace"]))
        }

        fun getDeviceIdMapping(from: String) = database.getCollection<DeviceIdMapping>().find(DeviceIdMapping::from eq from).singleOrNull()
    }

}