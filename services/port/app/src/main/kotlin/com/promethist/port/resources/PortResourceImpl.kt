package com.promethist.port.resources

import com.promethist.core.Request
import com.promethist.core.Response
import com.promethist.core.resources.CoreResource
import com.promethist.port.PortService
import com.promethist.util.LoggerDelegate
import org.bson.types.ObjectId
import javax.inject.Inject
import javax.ws.rs.*
import javax.ws.rs.core.Response as JerseyResponse
import javax.ws.rs.core.StreamingOutput

@Path("/")
class PortResourceImpl : PortResource {

    private val logger by LoggerDelegate()

    /**
     * Example of dependency injection
     * @see com.promethist.port.Application constructor
     */
    @Inject
    lateinit var coreResource: CoreResource

    @Inject
    lateinit var dataService: PortService

    override fun process(request: Request): Response = coreResource.process(request)

    /*
    override fun messageQueuePush(appKey: String, message: Message): Boolean {
        return dataService.pushMessage(appKey, message)
    }

    override fun messageQueuePop(appKey: String, recipient: String, limit: Int): List<Message> {
        return dataService.popMessages(appKey, recipient, limit)
    }
    */

    override fun readFile(id: String): JerseyResponse {
        val file = dataService.getResourceFile(ObjectId(id))
        return JerseyResponse.ok(
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