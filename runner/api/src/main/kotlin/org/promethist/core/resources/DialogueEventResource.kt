package org.promethist.core.resources

import io.swagger.annotations.Api
import org.litote.kmongo.Id
import org.promethist.core.model.DialogueEvent
import javax.ws.rs.GET
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["Dialogue Event"])
@Produces(MediaType.APPLICATION_JSON)
interface DialogueEventResource {
    @GET
    fun getDialogueEvents():List<DialogueEvent>

    fun get(eventId: Id<DialogueEvent>): DialogueEvent?
    fun create(dialogueEvent: DialogueEvent)
}