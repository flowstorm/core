package com.promethistai.port.resources

import com.promethistai.core.model.Message
import com.promethistai.core.resources.BotService
import com.promethistai.port.DataService
import com.promethistai.port.model.Contract
import com.promethistai.port.tts.TtsRequest
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

        val response = botService.message(appKey, message.apply {
            sessionId = if (sessionId.isNullOrBlank()) { Message.createId() } else { sessionId }
        })
        if (response != null && response._ref == null)
            response._ref = message._id

        return response
    }

    override fun messageQueuePush(appKey: String, message: Message): Boolean {
        return dataService.pushMessage(appKey, message)
    }

    override fun messageQueuePop(appKey: String, recipient: String, limit: Int): List<Message> {
        return dataService.popMessages(appKey, recipient, limit)
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

    override fun tts(appKey: String, ttsRequest: TtsRequest): ByteArray {
        val contract = getContract(appKey)
        return dataService.getTtsAudio(ttsRequest, true, true).speak().data!!
    }
}