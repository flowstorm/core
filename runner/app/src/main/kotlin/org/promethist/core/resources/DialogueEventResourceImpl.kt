package org.promethist.core.resources

import org.litote.kmongo.*
import org.promethist.common.resources.EntityResourceBase
import org.promethist.common.security.Authorized
import org.promethist.core.model.DialogueEvent
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("/dialogueEvent")
@Produces(MediaType.APPLICATION_JSON)
@Authorized
class DialogueEventResourceImpl: DialogueEventResource, EntityResourceBase<DialogueEvent>() {

    override val collection by lazy { database.getCollection<DialogueEvent>() }

    override fun list() = collection.aggregate(seek<DialogueEvent>()).toMutableList()

    override fun create(dialogueEvent: DialogueEvent) {
        collection.insertOne(dialogueEvent)
    }
}