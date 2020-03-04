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

            val session = initSession(message.sessionId ?: error("no session id"))
            sessionResource.update(session)

            if (session.user == null) {
                val usercontent = contentDistributionResource.resolve(message.sender!!)
                session.user = usercontent.user
                if (session.application == null) {
                    session.application = selectApplication(message, appKey, usercontent.applications)
                }
            }

            val user = session.user!!
            val app = session.application!!


//            if (appKey.contains("::")) {
//                // specific dialogue requested by name = demo/editor run mode
//                message.recipient = appKey.substringAfter("::")
//                addUserToExtensions(message, user)
//                val response = dialogueResouce.message(appKey, message)!!
//                val ttsVoice = TtsConfig.defaultVoice(message.language?.language ?: "en")
//
//                return response.apply { this.items.forEach { it.ttsVoice = it.ttsVoice ?: ttsVoice } }
//
//            }

//            val (session, user, app) = initializeSession(appKey, message)

            val appVariables = mutableMapOf<String, Serializable>()
//            appVariables[app.mainDialog.name] = app.mainDialog.modifiableVariables
//            for (model in app.subDialogs) {
//                appVariables[model.name] = model.modifiableVariables
//            }

            addUserToExtensions(message, user)
//            message.attributes["modelMain"] = app.mainDialog.name as Serializable
//            message.attributes["modelOrder"] = app.subDialogs.map { model -> model.name }.toList() as Serializable
            message.attributes["variables"] = appVariables as Serializable
            message.recipient = app.dialogueName

//            val ttsVoice = app.ttsVoice ?: TtsConfig.defaultVoice("en")


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

            response.apply { this.items.forEach { it.ttsVoice = it.ttsVoice ?: app.ttsVoice } }
            return response
        } catch (e: Exception) {
            return getErrorMessageResponse(message, e)
        }
    }

    private fun initSession(sessionId: String): Session {
        sessionResource.get(sessionId)?.let {
            logger.info("Restoring the existing session.")
            return it
        }

        logger.info("Starting a new session.")
        return Session(sessionId = sessionId)
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

//    private fun initializeSession(appKey: String, message: Message): Triple<Session, User, Application> {
//        var session = sessionResource.get(message.sessionId!!)
//
//        if (session != null) {
//            logger.info("Restoring the existing session.")
//            return Triple(session,
//                    userResource.getUser(session.user_id),
//                    applicationResource.getApplication(session.applicationId))
//        }
//
//        logger.info("Starting a new session.")
//        val clientIdentity = identifyClient(message.sender!!)
//        val applications = getAssignedApplications(clientIdentity)
//        val app = selectApplication(message, appKey, applications)
//        session = Session(sessionId = message.sessionId!!, applicationId = app._id, user_id = clientIdentity.first._id, organization_id = app.organization_id)
//        sessionResource.create(session)
//
//        return Triple(session, clientIdentity.first, app)
//    }

//    private fun identifyClient(sender: String): Pair<User, Device?> {
//        if (sender.startsWith("Bearer ")) {
//            val jwt = JwtToken.createFromHeaderString(sender)
//            return Pair(userResource.getUsers(username = jwt.username).firstOrNull()
//                    ?: throw UserNotFoundException("User not found ${jwt.username}."),
//                    null)
//        } else {
//            val device = deviceResource.getDevices().singleOrNull { device -> device.deviceId == sender }
//                    ?: throw DeviceNotFoundException("Device not found for sender=$sender")
//            if (device.user_id == null) throw DeviceNotAssignedException("Device is not assigned to a user.")
//
//            return Pair(userResource.getUser(device.user_id!!), device)
//        }
//    }

//    private fun getAssignedApplications(clientIdentity: Pair<User, Device?>): List<Application> {
//        val (user, device) = clientIdentity
//
//        return if (device != null) {
//            // get only applications from user assignment in organization
//            organizationResource.getApplicationsForUserInOrganization(organizationId = device.organization_id!!, userId = user._id)
//        } else {
//            // get all user's applications from all assignments
//            userResource.getApplications(user._id)
//        }
//    }

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
        // find application by action condition and language
        val apps = availableApplications.filter { application: Application ->
            val config = TtsConfig.forVoice(application.ttsVoice ?: "Grace")
            (application.startCondition == Application.StartCondition(Application.StartCondition.Type.OnAction, message.items[0].text
                    ?: "")) &&
                    ((application.ttsVoice == null) || (message.language == null) || (config.language.substring(0, 2) == message.language?.language))
        }
        if (apps.isEmpty()) throw NotFoundException("There is no assigned application fulfilling required conditions.(language, voice, start condition)")

        return apps.random()
    }

    class DeviceNotFoundException(message: String?) : NotFoundException(message)
    class DeviceNotAssignedException(message: String?) : NotFoundException(message)
    class UserNotFoundException(message: String?) : NotFoundException(message)
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