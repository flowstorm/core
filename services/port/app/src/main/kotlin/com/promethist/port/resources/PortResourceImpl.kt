package com.promethist.port.resources

import com.promethist.core.model.Message
import com.promethist.core.resources.BotService
import com.promethist.port.PortService
import com.promethist.util.LoggerDelegate
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import javax.inject.Inject
import javax.ws.rs.*
import javax.ws.rs.core.Response
import javax.ws.rs.core.StreamingOutput

@Path("/")
class PortResourceImpl : PortResource {

    private val logger by LoggerDelegate()

    /**
     * Example of dependency injection
     * @see com.promethist.port.Application constructor
     */
    @Inject
    lateinit var botService: BotService

    @Inject
    lateinit var dataService: PortService

    override fun message(appKey: String, message: Message): Message? {
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
}