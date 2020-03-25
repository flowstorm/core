package com.promethist.core.resources

import com.promethist.core.nlp.Context
import com.promethist.core.context.ContextFactory
import com.promethist.core.model.*
import com.promethist.core.nlp.Pipeline
import com.promethist.core.profile.ProfileRepository
import com.promethist.core.resources.ContentDistributionResource.ContentRequest
import org.slf4j.LoggerFactory
import java.io.Serializable
import javax.inject.Inject
import javax.ws.rs.*
import java.util.*

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
    lateinit var profileRepository: ProfileRepository

    private val czechLocale = Locale.forLanguageTag("cs")
    private var logger = LoggerFactory.getLogger(javaClass)

    override fun message(appKey: String, message: Message): Message? {

        val session = try {
            initSession(message, appKey)
        } catch (e: Exception) {
            return getErrorMessageResponse(message, e)
        }

        val context = contextFactory.createContext(session, message)

        val response = try {
            when (session.application.dialogueEngine) {
                "helena" -> getHelenaResponse(message, session, appKey)
                "core" -> {
                    val processedContext = processPipeline(context)
                    message.response(processedContext.turn.responseItems, context.sessionEnded)
                }
                else -> error("Unknown dialogue engine (${session.application.dialogueEngine})")
            }
        } catch (e: Exception) {
            getErrorMessageResponse(message, e)
        }

        return try {
            session.addMessage(response)
            sessionResource.update(session)
            response
        } catch (e: Exception) {
            getErrorMessageResponse(message, e)
        }
    }

    private fun processPipeline(context: Context): Context {
        val processedContext = nlpPipeline.process(context)
        persistContext(processedContext)
        return processedContext
    }

    private fun persistContext(context: Context) {
        context.session.turns.add(context.turn)
        sessionResource.update(context.session)
        profileRepository.save(context.profile)
        //todo metrics
    }

    private fun getHelenaResponse(message: Message, session: Session, appKey: String): Message {
        val appVariables = mutableMapOf<String, Serializable>()
        addUserToExtensions(message, session.user)
        message.attributes["variables"] = appVariables as Serializable
        message.recipient = session.application.dialogueName

        logger.info(message.toString())
        val response = dialogueResouce.message(appKey, message)!!
        logger.info(response.toString())
        response.apply { this.items.forEach { it.ttsVoice = it.ttsVoice ?: session.application.ttsVoice } }

        val metrics = if (response.attributes.containsKey("metrics"))
            response.attributes["metrics"] as Map<String, Any>
        else mapOf()
        updateMetrics(session, metrics)

        return response
    }

    private fun initSession(message: Message, appKey: String): Session {
        val sessionId = message.sessionId ?: error("No session id.")
        val storedSession = sessionResource.get(sessionId)
        val session = if (storedSession != null) {
            logger.info("Restoring the existing session.")
            storedSession
        } else {
            logger.info("Starting a new session.")
            val contentResponse = contentDistributionResource.resolve(
                    ContentRequest(
                            message.sender,
                            appKey,
                            message.language?.language,
                            Application.StartCondition(Application.StartCondition.Type.OnAction, message.items[0].text?: "")
                    ))

            Session(sessionId = sessionId, user = contentResponse.user, application = contentResponse.application, attributes = contentResponse.sessionAttributes)
        }

        session.addMessage(message)
        sessionResource.update(session)

        return session
    }

    private fun addUserToExtensions(message: Message, user: User) {
        message.sender = user.username
        message.attributes["user"] = Hashtable<String, String>(mapOf(
                "name" to user.name,
                "surname" to user.surname,
                "username" to user.username,
                "nickname" to user.nickname
        ))
        message.attributes["username"] = user.nickname
    }

    private fun getErrorMessageResponse(message: Message, e: Exception): Message {
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
        val items = mutableListOf<MessageItem>()
        if (message.language == czechLocale)
            items.add(MessageItem(ttsVoice = TtsConfig.defaultVoice("cs"), text = getErrorResourceString(czechLocale, "exception.$type", listOf(code))))
        else
            items.add(MessageItem(ttsVoice = TtsConfig.defaultVoice("en"), text = getErrorResourceString(Locale.ENGLISH, "exception.$type", listOf(code))))
        if (text != null)
            items.add(MessageItem(ttsVoice = TtsConfig.defaultVoice("en"), text = text))
        return message.response(items).apply { sessionEnded = true }
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