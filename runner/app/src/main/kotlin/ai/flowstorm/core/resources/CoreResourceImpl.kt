package ai.flowstorm.core.resources

import ch.qos.logback.classic.Level
import ai.flowstorm.core.*
import ai.flowstorm.core.model.*
import ai.flowstorm.core.runtime.ContextLog
import ai.flowstorm.core.runtime.PipelineRuntime
import ai.flowstorm.core.type.Dynamic
import ai.flowstorm.util.LoggerDelegate
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
    lateinit var contextLog: ContextLog

    @Inject
    lateinit var pipelineRuntime: PipelineRuntime

    private val logger by LoggerDelegate()

    override fun process(request: Request): Response = with(request) {

        //todo get logger level from request
        contextLog.level = Level.ALL

        val session = initSession(appKey, deviceId, token, sessionId, initiationId, input)

        val response = try {
            pipelineRuntime.process(session, request, contextLog)
        } catch (e: Throwable) {
            processException(request, e)
        }

        try {
            sessionResource.update(session)
            return response
        } catch (e: Throwable) {
            return processException(request, e)
        }
    }

    private fun initSession(key: String, deviceId: String, token: String?, sessionId: String, initiationId: String?, input: Input): Session {
        val storedSession = sessionResource.findBy(sessionId)
        val session = if (storedSession != null) {
            logger.info("Restoring the existing session")
            storedSession
        } else {
            logger.info("Starting a new session $sessionId")
            val contentResponse = contentDistributionResource.resolve(
                ContentDistributionResource.ContentRequest(deviceId, token, key, input.locale.language)
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
        contextLog.logger.error(DialogueEvent.toText(e))

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
        }, contextLog.log, mutableMapOf(), null, mutableListOf(), sessionEnded = true)
    }

    private fun getMessageResourceString(language: String, type: String, params: List<Any> = listOf()): String {
        val resourceBundle = ResourceBundle.getBundle("messages", Locale(language))
        val key = if (resourceBundle.containsKey(type)) type else "OTHER"

        return resourceBundle.getString(key).replace("\\{(\\d)\\}".toRegex()) {
            params[it.groupValues[1].toInt()].toString()
        }
    }
}