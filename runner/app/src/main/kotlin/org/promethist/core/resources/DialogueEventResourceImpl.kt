package org.promethist.core.resources

import org.litote.kmongo.*
import org.promethist.common.query.Query
import org.promethist.common.security.Authorized
import org.promethist.core.model.DialogueEvent
import org.promethist.core.repository.EventRepository
import javax.inject.Inject
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("/dialogueEvent")
@Produces(MediaType.APPLICATION_JSON)
@Authorized
class DialogueEventResourceImpl: DialogueEventResource {

    @Inject
    lateinit var eventRepository: EventRepository

    @Inject
    lateinit var query: Query

    override fun getDialogueEvents(): List<DialogueEvent> = eventRepository.find(query)

    override fun create(dialogueEvent: DialogueEvent) = eventRepository.create(dialogueEvent).let {  }

    override fun get(eventId: Id<DialogueEvent>): DialogueEvent = eventRepository.get(eventId)
}