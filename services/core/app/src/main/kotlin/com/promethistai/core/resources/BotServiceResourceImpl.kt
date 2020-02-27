package com.promethistai.core.resources

import com.promethistai.core.BotException
import com.promethistai.core.model.Application
import com.promethistai.core.model.Session
import com.promethistai.core.model.User
import com.promethistai.port.bot.BotService
import com.promethistai.port.model.Message
import com.promethistai.port.tts.TtsConfig
import org.slf4j.LoggerFactory
import javax.inject.Inject
import javax.ws.rs.*
import java.util.*
import kotlin.random.Random

@Path("/")
class BotServiceResourceImpl : BotService {

    @Inject
    lateinit var sessionResource: SessionResource

    private val czechLocale = Locale.forLanguageTag("cs")
    private var logger = LoggerFactory.getLogger(BotServiceResourceImpl::class.java)

    fun getAssignedApplications(sender: String): Pair<User, List<Application>> {
        TODO("call admin")
    }

    override fun message(key: String, message: Message): Message? {
        try {
            var session = sessionResource.get(message.sessionId!!)
            val assigned: Pair<User, List<Application>>
            var app: Application? = null

            if (session == null) {
                assigned = getAssignedApplications(message.sender!!)
                if (key.contains(':')) {
                    val spec = key.substring(key.indexOf(':') + 1)
                    if (spec.startsWith(':')) {
                        // select dialogue model by name
                        message.recipient = spec.substring(1)
                    } else {
                        // select application by name
                        app = assigned.second.find { application: Application -> spec == application.name }
                    }
                } else {
                    // find application by action condition and language
                    val apps = assigned.second.filter { application: Application ->
                        val config = TtsConfig.forVoice(application.ttsVoice?:"Grace")
                        /*(application.startCondition == Application.StartCondition(Application.StartCondition.Type.OnAction, message.items[0].text
                                ?: "")) &&*/
                                ((application.ttsVoice == null) || (message.language == null) || (config?.language?.substring(0, 2) == message.language?.language))
                    }
                    if (!apps.isEmpty())
                        app = apps[Random.nextInt(apps.size)]
                }
                if (app != null) {
                    session = Session(sessionId = message.sessionId!!, applicationId = app._id, user_id = assigned.first.§)
                    sessionResource.create(session)
                }
            } else {
                app = TODO("get application from admin by id")
            }

            val user = assigned.first
            message.sender = user.username
            message.extensions["name"] = user.name
            message.extensions["surname"] = user.surname
            message.extensions["nickname"] = user.nickname
            message.extensions["username"] = user.nickname //DEPRECATED

            val dialogueResouce: BotService = TODO("get helena")
            if (app == null) {
                if (message.recipient != null) {
                    // we have dialogue name = demo/editor run mode
                    val response = dialogueResouce.message(key, message)!!
                    val ttsVoice = TtsConfig.defaultVoice(message.language?.language?:"en")
                    return response.apply { this.items.forEach { it.ttsVoice = it.ttsVoice ?: ttsVoice } }
                } else {
                    throw BotException(BotException.Type.NO_APPLICATIONS, message.sender!!)
                }
            } else {
                /* TO BE REMOVED
                val subDialogs = app.subDialogs
                val appVariables = mutableMapOf<String, Serializable>()
                appVariables[app.mainDialog.name] = app.mainDialog.modifiableVariables
                for (model in subDialogs) {
                    appVariables[model.name] = model.modifiableVariables
                }

                message.extensions["modelMain"] = app.mainDialog.name as Serializable
                message.extensions["modelOrder"] = subDialogs.map { model -> model.name }.toList() as Serializable
                message.extensions["variables"] = appVariables as Serializable
                message.recipient = app.name
                */
                val ttsVoice = app.ttsVoice?:TtsConfig.defaultVoice("en")

                logger.info(message.toString())
                val response = dialogueResouce.message(key, message)!!
                logger.info(response.toString())

                val metrics = if (response.extensions.containsKey("metrics"))
                    response.extensions["metrics"] as Map<String, Any>
                else mapOf()

                session!!.addMessage(message)
                session!!.addMessage(response)
                updateMetrics(session!!, metrics)
                sessionResource.update(session)

                response.apply { this.items.forEach { it.ttsVoice = it.ttsVoice ?: ttsVoice } }
                return response
            }
        } catch (e: Exception) {
            return getErrorMessageResponse(message, e)
        }
    }

    private fun getErrorMessageResponse(message: Message, e: Exception): Message {
        var type = "OTHER"
        var code = 1
        var text: String? = null
        when (e) {
            is WebApplicationException -> {
                code = e.response.status
                text = e.response.readEntity(String::class.java)
            }
            is BotException ->
                type = e.type.toString()
            else ->
                text = e.message
        }
        logger.warn("getErrorMessageResponse(class = ${e.javaClass}, type = $type, code = $code, text = $text)")
        val items = mutableListOf<Message.Item>()
        if (message.language == czechLocale)
            items.add(Message.Item(ttsVoice = TtsConfig.defaultVoice("cs"), text = getString(czechLocale, "exception.$type", listOf(code))))
        else
            items.add(Message.Item(ttsVoice = TtsConfig.defaultVoice("en"), text = getString(Locale.ENGLISH, "exception.$type", listOf(code))))
        if (text != null)
            items.add(Message.Item(ttsVoice = TtsConfig.defaultVoice("en"), text = text))
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

    fun getString(locale: Locale, key: String, params: List<Any> = listOf()) =
            ResourceBundle.getBundle("resources", locale).getString(key).replace("\\{(\\d)\\}".toRegex()) {
                params[it.groupValues[1].toInt()].toString()
            }

}