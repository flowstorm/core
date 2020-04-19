package com.promethist.core.resources

import ch.qos.logback.classic.Level
import com.promethist.core.*
import com.promethist.core.context.ContextFactory
import com.promethist.core.context.ContextPersister
import com.promethist.core.model.*
import com.promethist.core.model.Application
import com.promethist.core.model.metrics.Metric
import com.promethist.core.resources.ContentDistributionResource.ContentRequest
import com.promethist.core.runtime.DialogueLog
import com.promethist.core.type.PropertyMap
import com.promethist.util.LoggerDelegate
import java.io.Serializable
import javax.inject.Inject
import javax.ws.rs.*
import java.util.*
import kotlin.collections.toList

@Path("/")
class CoreResourceImpl : CoreResource {

    @Inject
    lateinit var contentDistributionResource: ContentDistributionResource

    @Inject
    lateinit var sessionResource: SessionResource

    @Inject
    lateinit var dialogueResouce: BotService

    @Inject
    lateinit var pipelineFactory: PipelineFactory

    @Inject
    lateinit var contextFactory: ContextFactory

    @Inject
    lateinit var dialogueLog: DialogueLog

    @Inject
    lateinit var contextPersister: ContextPersister

    private val czechLocale = Locale.forLanguageTag("cs")
    private val logger by LoggerDelegate()

    override fun process(request: Request): Response = with(request) {

        //todo get logger level from request
        dialogueLog.level = Level.ALL

        val session = try {
            initSession(appKey, sender, sessionId, input)
        } catch (e: Exception) {
            return processException(input, e)
        }

        updateAutomaticMetrics(session)

        val response = try {
            when (session.application.dialogueEngine) {
                "helena" -> getHelenaResponse(appKey, sender, session, input)
                "core" -> {
                    val pipeline = pipelineFactory.createPipeline()
                    val context = contextFactory.createContext(pipeline, session, input)
                    with (processPipeline(context)) {
                        // client attributes
                        listOf("speakingRate", "speakingPitch", "speakingVolumeGain").forEach {
                            if (!turn.attributes.containsKey(it)) {
                                val value = session.attributes[it] ?: profile.attributes[it]
                                if (value != null)
                                    turn.attributes[it] = value
                            }
                        }
                        Response(turn.responseItems, dialogueLog.log, turn.attributes, expectedPhrases, sessionEnded)
                    }
                }
                else -> error("Unknown dialogue engine (${session.application.dialogueEngine})")
            }
        } catch (e: Exception) {
            processException(input, e)
        }

        return try {
            session.addMessage(Session.Message(Date(), sender, null, response.items))
            sessionResource.update(session)
            response
        } catch (e: Exception) {
            return processException(input, e)
        }
    }

    private fun processPipeline(context: Context): Context {
        val processedContext = context.pipeline.process(context)
        contextPersister.persist(processedContext)
        return processedContext
    }

    private fun getHelenaResponse(key: String, sender: String, session: Session, input: Input): Response {
        val requestMessage = Message(
                sender = sender,
                recipient = session.application.dialogueName,
                sessionId = session.sessionId,
                items = mutableListOf(Response.Item(text = input.transcript.text)))
        val appVariables = mutableMapOf<String, Serializable>()
        addUserToExtensions(requestMessage, session.user)
        requestMessage.attributes["variables"] = appVariables as Serializable

        logger.info(requestMessage.toString())
        val responseMessage = dialogueResouce.message(key, requestMessage)!!
        logger.info(responseMessage.toString())
        responseMessage.apply { this.items.forEach { it.ttsVoice = it.ttsVoice ?: session.application.ttsVoice } }

        val metrics = if (responseMessage.attributes.containsKey("metrics"))
            @Suppress("UNCHECKED_CAST") //suppressed, will be removed anyway
            responseMessage.attributes["metrics"] as PropertyMap
        else mapOf()
        updateMetrics(session, metrics)

        dialogueLog.logger.info("passed nodes " + responseMessage.attributes["passedNodes"])

        return Response(responseMessage.items, dialogueLog.log,
                responseMessage.attributes, responseMessage.expectedPhrases, responseMessage.sessionEnded)
    }

    private fun initSession(key: String, sender: String, sessionId: String, input: Input): Session {
        val storedSession = sessionResource.get(sessionId)
        val session = if (storedSession != null) {
            logger.info("Restoring the existing session.")
            storedSession
        } else {
            logger.info("Starting a new session.")
            val contentResponse = contentDistributionResource.resolve(
                    ContentRequest(
                            sender,
                            key,
                            input.locale.toString(),
                            Application.StartCondition(Application.StartCondition.Type.OnAction, input.transcript.text)
                    ))

            Session(sessionId = sessionId, user = contentResponse.user, application = contentResponse.application, properties = contentResponse.sessionProperties)
        }

        session.addMessage(Session.Message(Date(), sender, null, mutableListOf(Response.Item(text = input.transcript.text))))
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

    private fun addUserToExtensions(message: Message, user: User) {
        message.sender = user.username
        message.attributes["user"] = Hashtable(mapOf(
                "name" to user.name,
                "surname" to user.surname,
                "username" to user.username,
                "nickname" to user.nickname
        ))
        message.attributes["username"] = user.nickname
    }

    private fun processException(input: Input, e: Exception): Response {
        val type = e::class.simpleName
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
        val logText = "class = ${e.javaClass}, type = $type, code = $code, text = $text"
        logger.warn("getErrorMessageResponse($logText)")
        val items = mutableListOf<Response.Item>()
        if (input.locale == czechLocale)
            items.add(Response.Item(ttsVoice = TtsConfig.defaultVoice("cs"), text = getErrorResourceString(czechLocale, "exception.$type", listOf(code))))
        else
            items.add(Response.Item(ttsVoice = TtsConfig.defaultVoice("en"), text = getErrorResourceString(Locale.ENGLISH, "exception.$type", listOf(code))))
        if (text != null)
            items.add(Response.Item(ttsVoice = TtsConfig.defaultVoice("en"), text = text))

        dialogueLog.logger.error(logText)

        return Response(items, dialogueLog.log, mutableMapOf<String, Any>(), mutableListOf(), sessionEnded = true)
    }

    private fun updateMetrics(session: Session, metrics: PropertyMap) {
        for (namespaceMetrics in metrics) {
            if (namespaceMetrics.value is Message.PropertyMap) {
                @Suppress("UNCHECKED_CAST") //suppressed, will be removed anyway
                updateMetricsValues(session, namespaceMetrics.key, namespaceMetrics.value as PropertyMap)
            } else {
                //TODO warning
            }
        }
    }

    private fun updateMetricsValues(session: Session, namespace: String, metrics: PropertyMap) {
        for (item in metrics) {
            if (item.value is Long) {
                val m = session.metrics.filter { it.namespace == namespace && it.name == item.key }.firstOrNull()
                if (m != null) {
                    m.value = item.value as Long
                } else {
                    val metric = Metric(namespace, item.key, item.value as Long)
                    session.metrics.add(metric)
                }
            } else {
                //TODO warning
            }
        }
    }

    private fun getErrorResourceString(locale: Locale, type: String, params: List<Any> = listOf()): String {
        val resourceBundle = ResourceBundle.getBundle("errors", locale)
        val key = if (resourceBundle.containsKey(type)) type else "OTHER"

        return resourceBundle.getString(key).replace("\\{(\\d)\\}".toRegex()) {
            params[it.groupValues[1].toInt()].toString()
        }
    }
}