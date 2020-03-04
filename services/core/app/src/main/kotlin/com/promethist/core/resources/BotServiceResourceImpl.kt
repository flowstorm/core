package com.promethist.core.resources

import com.promethist.core.model.*
import org.slf4j.LoggerFactory
import java.io.Serializable
import javax.inject.Inject
import javax.ws.rs.*
import java.util.*

@Path("/")
class BotServiceResourceImpl : BotService {

    @Inject
    lateinit var contentDistributionResource: ContentDistributionResource

    @Inject
    lateinit var sessionResource: SessionResource

    @Inject
    lateinit var dialogueResouce: BotService

    private val czechLocale = Locale.forLanguageTag("cs")
    private var logger = LoggerFactory.getLogger(BotServiceResourceImpl::class.java)

    override fun message(appKey: String, message: Message): Message? {
        try {

            val sessionId = message.sessionId ?: error("No session id")
            val storedSession = sessionResource.get(sessionId)
            val session = if (storedSession != null) {
                logger.info("Restoring the existing session.")
                storedSession
            } else {
                val userContent = contentDistributionResource.resolve(message.sender!!)
                logger.info("Starting a new session.")
                Session(sessionId = sessionId, user = userContent.user, application = selectApplication(message, appKey, userContent.applications))
                }


            val appVariables = mutableMapOf<String, Serializable>()
            addUserToExtensions(message, session.user)
            message.attributes["variables"] = appVariables as Serializable
            message.recipient = session.application.dialogueName

            logger.info(message.toString())
            val response = dialogueResouce.message(appKey, message)!!
            logger.info(response.toString())

            val metrics = if (response.attributes.containsKey("metrics"))
                response.attributes["metrics"] as Map<String, Any>
            else mapOf()

            session.addMessage(message)
            session.addMessage(response)
            updateMetrics(session, metrics)
            sessionResource.update(session)

            response.apply { this.items.forEach { it.ttsVoice = it.ttsVoice ?: session.application.ttsVoice } }
            return response
        } catch (e: Exception) {
            return getErrorMessageResponse(message, e)
        }
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

    private fun selectApplication(message: Message, appKey: String, availableApplications: List<Application>): Application {
        if (appKey.contains("::")) {
            return Application(
                    name = "Dialog specified in appKey",
                    dialogueName = appKey.substringAfter("::"),
                    ttsVoice = TtsConfig.defaultVoice(message.language?.language ?: "en")
            )
        }

        if (availableApplications.isEmpty()) throw NoApplicationException("There are no assigned application for the user.")

        if (appKey.contains(':')) {
            logger.info("Loading application using appKey $appKey")
            val spec = appKey.substringAfter(":")
            // select application by id
            return availableApplications.find { application: Application -> spec == application._id.toString() }
                    ?: throw NoApplicationException("Requested application is not in the list of assigned applications. Requested appKey=$appKey.")
        }

        logger.info("Selecting application using given conditions $appKey")
        // select application by action condition and language
        val apps = availableApplications.filter { application: Application ->
            val config = TtsConfig.forVoice(application.ttsVoice ?: "Grace")
            (application.startCondition == Application.StartCondition(Application.StartCondition.Type.OnAction, message.items[0].text
                    ?: "")) &&
                    ((message.language == null) || (config.language.substring(0, 2) == message.language?.language))
        }
        if (apps.isEmpty()) throw NoApplicationException("There is no assigned application fulfilling required conditions.(language, voice, start condition)")

        return apps.random()
    }

    class NoApplicationException(message: String?) : NotFoundException(message)

    private fun getErrorMessageResponse(message: Message, e: Exception): Message {
        val type = e::class.simpleName
        var code = 1
        var text: String? = null
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