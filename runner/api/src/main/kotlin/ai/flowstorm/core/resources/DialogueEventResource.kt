package ai.flowstorm.core.resources

import io.swagger.annotations.Api
import org.litote.kmongo.Id
import ai.flowstorm.core.model.DialogueEvent
import ai.flowstorm.core.repository.DialogueEventRepository
import ai.flowstorm.security.Authenticated
import javax.ws.rs.GET
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["Dialogue Event"])
@Produces(MediaType.APPLICATION_JSON)
interface DialogueEventResource : DialogueEventRepository {
    @GET
    fun list(): List<DialogueEvent>

    fun get(eventId: Id<DialogueEvent>): DialogueEvent?
    override fun create(dialogueEvent: DialogueEvent)
}