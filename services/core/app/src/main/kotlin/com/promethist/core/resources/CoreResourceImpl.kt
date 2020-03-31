package com.promethist.core.resources

import com.promethist.core.*
import com.promethist.core.context.ContextFactory
import com.promethist.core.context.ContextPersister
import com.promethist.core.model.*
import com.promethist.core.model.Application
import com.promethist.core.resources.ContentDistributionResource.ContentRequest
import org.slf4j.LoggerFactory
import java.io.Serializable
import javax.inject.Inject
import javax.ws.rs.*
import java.util.*
import kotlin.collections.LinkedHashMap

@Path("/")
class CoreResourceImpl : CoreResource {

    @Inject
    lateinit var contentDistributionResource: ContentDistributionResource

    @Inject
    lateinit var sessionResource: SessionResource

    @Inject
    lateinit var dialogueResouce: BotService

    @Inject
    lateinit var nlpPipeline: Pipeline

    @Inject
    lateinit var contextFactory: ContextFactory

    @Inject
    lateinit var contextPersister: ContextPersister

    private val czechLocale = Locale.forLanguageTag("cs")
    private var logger = LoggerFactory.getLogger(javaClass)

    override fun process(request: Request): Response = with(request) {

        val session = try {
            initSession(key, sender, sessionId, input)
        } catch (e: Exception) {
            return processException(input, e)
        }

        val response = try {
            when (session.application.dialogueEngine) {
                "helena" -> getHelenaResponse(key, sender, session, input)
                "core" -> {
                    val context = contextFactory.createContext(session, input)
                    val processedContext = processPipeline(context)
                    Response(processedContext.turn.responseItems, processedContext.turn.attributes, context.sessionEnded)
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
        val processedContext = nlpPipeline.process(context)
        contextPersister.persist(processedContext)
        return processedContext
    }

    private fun getHelenaResponse(key: String, sender: String, session: Session, input: Input): Response {
        val message = Message(
                sender = sender,
                recipient = session.application.dialogueName,
                sessionId = session.sessionId,
                items = mutableListOf(Response.Item(text = input.transcript.text)))
        val appVariables = mutableMapOf<String, Serializable>()
        addUserToExtensions(message, session.user)
        message.attributes["variables"] = appVariables as Serializable

        logger.info(message.toString())
        val response = dialogueResouce.message(key, message)!!
        logger.info(response.toString())
        response.apply { this.items.forEach { it.ttsVoice = it.ttsVoice ?: session.application.ttsVoice } }

        val metrics = if (response.attributes.containsKey("metrics"))
            response.attributes["metrics"] as Map<String, Any>
        else mapOf()
        updateMetrics(session, metrics)

        return Response(response.items, response.attributes, response.sessionEnded)
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
                            input.language.toString(),
                            Application.StartCondition(Application.StartCondition.Type.OnAction, input.transcript.text)
                    ))

            Session(sessionId = sessionId, user = contentResponse.user, application = contentResponse.application, attributes = contentResponse.sessionAttributes)
        }

        session.addMessage(Session.Message(Date(), sender, null, mutableListOf(Response.Item(text = input.transcript.text))))
        sessionResource.update(session)

        return session
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
                text = e.message
        }
        logger.warn("getErrorMessageResponse(class = ${e.javaClass}, type = $type, code = $code, text = $text)")
        val items = mutableListOf<Response.Item>()
        if (input.language == czechLocale)
            items.add(Response.Item(ttsVoice = TtsConfig.defaultVoice("cs"), text = getErrorResourceString(czechLocale, "exception.$type", listOf(code))))
        else
            items.add(Response.Item(ttsVoice = TtsConfig.defaultVoice("en"), text = getErrorResourceString(Locale.ENGLISH, "exception.$type", listOf(code))))
        if (text != null)
            items.add(Response.Item(ttsVoice = TtsConfig.defaultVoice("en"), text = text))
        return Response(items, mutableMapOf<String, Any>(), true)
    }

    private fun updateMetrics(session: Session, metrics: Map<String, Any>) {
        session.metrics.find { it.name == "TurnCount" && it.namespace == "session" }?.increment()
                ?: session.metrics.add(Session.Metric("session", "TurnCount"))

        for (namespaceMetrics in metrics) {
            if (namespaceMetrics.value is Message.PropertyMap) {
                updateMetricsValues(session, namespaceMetrics.key, namespaceMetrics.value as Map<String, Any>)
            } else {
                //TODO warning
            }
        }
    }

    private fun updateMetricsValues(session: Session, namespace: String, metrics: Map<String, Any>) {
        for (item in metrics) {
            if (item.value is Long) {
                val m = session.metrics.filter { it.namespace == namespace && it.name == item.key }.firstOrNull()
                if (m != null) {
                    m.value = item.value as Long
                } else {
                    val metric = Session.Metric(namespace, item.key, item.value as Long)
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