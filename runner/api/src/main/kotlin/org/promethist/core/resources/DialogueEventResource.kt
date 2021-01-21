package org.promethist.core.resources

import io.swagger.annotations.Api
import org.litote.kmongo.Id
import org.promethist.core.model.DialogueEvent
import org.promethist.core.repository.DialogueEventRepository
import org.promethist.security.Authenticated
import javax.ws.rs.GET
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["Dialogue Event"])
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
interface DialogueEventResource : DialogueEventRepository {
    @GET
    fun getDialogueEvents(): List<DialogueEvent>

    fun get(eventId: Id<DialogueEvent>): DialogueEvent?
    override fun create(dialogueEvent: DialogueEvent)
}