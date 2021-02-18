package org.promethist.core.resources

import ch.qos.logback.classic.Level
import org.promethist.common.monitoring.Monitor
import org.promethist.core.*
import org.promethist.core.context.ContextFactory
import org.promethist.core.context.ContextPersister
import org.promethist.core.dialogue.AbstractDialogue
import org.promethist.core.model.*
import org.promethist.core.model.metrics.Metric
import org.promethist.core.resources.ContentDistributionResource.ContentRequest
import org.promethist.core.runtime.DialogueLog
import org.promethist.core.type.Dynamic
import org.promethist.core.type.Memory
import org.promethist.util.LoggerDelegate
import java.util.*
import javax.inject.Inject
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.WebApplicationException
import javax.ws.rs.core.MediaType

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
class CoreResourceImpl : CoreResource {

    @Inject
    lateinit var contentDistributionResource: ContentDistributionResource

    @Inject
    lateinit var sessionResource: SessionResource

    @Inject
    lateinit var pairingResource: DevicePairingResource

    @Inject
    lateinit var dialogueEventResource: DialogueEventResource

    @Inject
    lateinit var pipelineFactory: PipelineFactory

    @Inject
    lateinit var contextFactory: ContextFactory

    @Inject
    lateinit var dialogueLog: DialogueLog

    @Inject
    lateinit var contextPersister: ContextPersister

    @Inject
    lateinit var monitor: Monitor

    private val logger by LoggerDelegate()

    override fun process(request: Request): Response = with(request) {

        //todo get logger level from request
        dialogueLog.level = Level.ALL

        val session = try {
            initSession(appKey, deviceId, token, sessionId, initiationId, input)
        } catch (e: Exception) {
            return processException(request, e)
        }

        updateAutomaticMetrics(session)

        val response = try {
            val pipeline = pipelineFactory.createPipeline()
            val context = contextFactory.createContext(pipeline, session, request)
            with (processPipeline(context)) {
                // client attributes
                listOf("speakingRate", "speakingPitch", "speakingVolumeGain").forEach {
                    if (!turn.attributes[AbstractDialogue.defaultNamespace].containsKey(it)) {
                        val value = session.attributes[AbstractDialogue.defaultNamespace][it]
                                ?: userProfile.attributes[AbstractDialogue.defaultNamespace][it]
                        if (value != null)
                            turn.attributes[AbstractDialogue.defaultNamespace][it] = value
                    }
                }
                turn.responseItems.forEach {
                    it.ttsConfig = it.ttsConfig ?: Voice.forLanguage(locale?.language ?: "en").config
                }
                Response(locale, turn.responseItems, dialogueLog.log,
                        turn.attributes[AbstractDialogue.defaultNamespace].map { it.key to (it.value as Memory<*>).value }.toMap().toMutableMap(),
                        turn.sttMode, turn.expectedPhrases, sessionEnded, sleepTimeout)
            }
        } catch (e: Throwable) {
            processException(request, e)
        }

        return try {
            sessionResource.update(session)
            response
        } catch (e: Throwable) {
            return processException(request, e)
        }
    }

    private fun processPipeline(context: Context): Context {
        try {
            val processedContext = context.pipeline.process(context)
            contextPersister.persist(processedContext)
            return processedContext
        } catch (e: Throwable) {
            context.createDialogueEvent(e)
            monitor.capture(e)
            throw e
        }
    }

    private fun initSession(key: String, deviceId: String, token: String?, sessionId: String, initiationId: String?, input: Input): Session {
        val storedSession = sessionResource.findBy(sessionId)
        val session = if (storedSession != null) {
            logger.info("Restoring the existing session")
            storedSession
        } else {
            logger.info("Starting a new session")
            val contentResponse = contentDistributionResource.resolve(
                    ContentRequest(deviceId, token, key, input.locale.language)
            )
            Session(
                sessionId = sessionId,
                initiationId = initiationId,
                device = contentResponse.device,
                user = contentResponse.user,
                test = contentResponse.test,
                application = contentResponse.application,
                properties = Dynamic(contentResponse.sessionProperties),
                space_id = contentResponse.space._id
            )
        }
        sessionResource.update(session)
        return session
    }

    private fun updateAutomaticMetrics(session: Session) {
        with(session.metrics) {
            find { it.name == "count" && it.namespace == "session" }
                    ?: add(Metric("session", "count", 1))
            find { it.name == "turns" && it.namespace == "session" }?.increment()
                    ?: add(Metric("session", "turns", 1))
        }
    }

    private fun processException(request: Request, e: Throwable): Response {
        val type = e::class.simpleName!!
        var code = 1
        val text: String?
        e.printStackTrace()
        when (e) {
            is WebApplicationException -> {
                code = e.response.status
                text = if (e.response.hasEntity())
                    e.response.readEntity(String::class.java)
                else
                    e.message
            }
            else ->
                text = (e.cause?:e).message
        }
        dialogueLog.logger.error(DialogueEvent.toText(e))

        return Response(request.input.locale, mutableListOf<Response.Item>().apply {
            if (text?.startsWith("admin:NotFoundException: Device") == true) {
                val devicePairing = DevicePairing(deviceId = request.deviceId)
                pairingResource.createOrUpdateDevicePairing(devicePairing)
                val pairingCode = devicePairing.pairingCode.toCharArray().joinToString(", ")
                if (request.input.locale.language == "cs")
                    add(Response.Item(ttsConfig = Voice.forLanguage("cs").config,
                        text = getMessageResourceString("cs", "PAIRING", listOf(pairingCode))))
                else
                    add(Response.Item(ttsConfig = Voice.forLanguage("en").config,
                        text = getMessageResourceString("en", "PAIRING", listOf(pairingCode))))
            } else {
                if (request.input.locale.language == "cs")
                    add(Response.Item(ttsConfig = Voice.forLanguage("cs").config,
                        text = getMessageResourceString("cs", type, listOf(code))))
                else
                    add(Response.Item(ttsConfig = Voice.forLanguage("en").config,
                        text = getMessageResourceString("en", type, listOf(code))))
                if (text != null)
                    add(Response.Item(ttsConfig = Voice.forLanguage("en").config,
                        text = text))
            }
        }, dialogueLog.log, mutableMapOf(), null, mutableListOf(), sessionEnded = true)
    }

    private fun getMessageResourceString(language: String, type: String, params: List<Any> = listOf()): String {
        val resourceBundle = ResourceBundle.getBundle("messages", Locale(language))
        val key = if (resourceBundle.containsKey(type)) type else "OTHER"

        return resourceBundle.getString(key).replace("\\{(\\d)\\}".toRegex()) {
            params[it.groupValues[1].toInt()].toString()
        }
    }
}