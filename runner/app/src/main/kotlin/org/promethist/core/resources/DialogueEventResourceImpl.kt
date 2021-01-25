package org.promethist.core.resources

import org.litote.kmongo.*
import org.promethist.common.query.Query
import org.promethist.core.model.DialogueEvent
import org.promethist.core.repository.EventRepository
import javax.inject.Inject
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("/dialogueEvent")
@Produces(MediaType.APPLICATION_JSON)
class DialogueEventResourceImpl: DialogueEventResource {

    @Inject
    lateinit var eventRepository: EventRepository

    @Inject
    lateinit var query: Query

    override fun getDialogueEvents(): List<DialogueEvent> {
        return eventRepository.getDialogueEvents(query)
    }

    override fun create(dialogueEvent: DialogueEvent) {
        eventRepository.create(dialogueEvent)
    }

    override fun get(eventId: Id<DialogueEvent>): DialogueEvent? = eventRepository.get(eventId)
}