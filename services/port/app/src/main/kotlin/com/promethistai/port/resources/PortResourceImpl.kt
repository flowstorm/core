package com.promethistai.port.resources

import com.promethistai.port.DataService
import com.promethistai.port.bot.BotService
import com.promethistai.port.model.Contract
import com.promethistai.port.model.Message
import com.promethistai.port.tts.TtsConfig
import com.promethistai.port.tts.TtsRequest
import com.promethistai.port.tts.TtsServiceFactory
import com.promethistai.port.tts.TtsVoice
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import javax.inject.Inject
import javax.ws.rs.*
import javax.ws.rs.core.Response
import javax.ws.rs.core.StreamingOutput

@Path("/")
class PortResourceImpl : PortResource {

    private var logger = LoggerFactory.getLogger(PortResourceImpl::class.java)

    /**
     * Example of dependency injection
     * @see com.promethistai.port.Application constructor
     */
    @Inject
    lateinit var botService: BotService

    @Inject
    lateinit var dataService: DataService


    override fun getContract(appKey: String): Contract {
        val contract = dataService.getContract(appKey)
        contract.botKey = "*****" // mask api key
        return contract
    }

    override fun message(appKey: String, message: Message): Message? {
        message.appKey = appKey

        // log incoming message
        dataService.logMessage(message)

        val response = botService.message(appKey, message.apply {
            sessionId = if (sessionId.isNullOrBlank()) { Message.createId() } else { sessionId }
        })
        if (response != null && response._ref == null)
            response._ref = message._id

        // log response message
        if (response != null)
            dataService.logMessage(response)

        return response
    }

    override fun messageQueuePush(appKey: String, message: Message): Boolean {
        return dataService.pushMessage(appKey, message)
    }

    override fun messageQueuePop(appKey: String, recipient: String, limit: Int): List<Message> {
        return dataService.popMessages(appKey, recipient, limit).onEach { dataService.logMessage(it) }
    }

    override fun readFile(id: String): Response {
        val file = dataService.getResourceFile(ObjectId(id))
        return Response.ok(
                    StreamingOutput { output ->
                        try {
                            file.download(output)
                        } catch (e: Exception) {
                            throw WebApplicationException("File streaming failed", e)
                        }
                    }, file.type)
                .header("Content-Disposition", "inline" + if (file.name == null) "" else "; filename=\"${file.name}\"")
                .build()
    }

    override fun tts(appKey: String, provider: String, ttsConfig: TtsConfig?, speechText: String): ByteArray {
        val contract = getContract(appKey)
        val ttsRequest = TtsRequest(text = speechText)
        ttsRequest.set(contract.ttsConfig?:TtsConfig.DEFAULT_EN)
        logger.info("tts(provider = $provider, ttsRequest = $ttsRequest)")
        return dataService.getTtsAudio(provider, ttsRequest, true, true).speak().data!!
    }

    override fun ttsVoices(provider: String): List<TtsVoice> {
        if (logger.isInfoEnabled)
            logger.info("ttsVoices(provider = $provider)")
        return TtsServiceFactory.create(provider).voices
    }

}