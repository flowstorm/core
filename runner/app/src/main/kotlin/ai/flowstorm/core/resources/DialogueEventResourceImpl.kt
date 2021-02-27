package ai.flowstorm.core.resources

import org.litote.kmongo.*
import ai.flowstorm.common.query.Query
import ai.flowstorm.common.security.Authorized
import ai.flowstorm.core.model.DialogueEvent
import ai.flowstorm.core.repository.EventRepository
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

    override fun list(): List<DialogueEvent> = eventRepository.find(query)

    override fun create(dialogueEvent: DialogueEvent) = eventRepository.create(dialogueEvent).let {  }

    override fun get(eventId: Id<DialogueEvent>): DialogueEvent = eventRepository.get(eventId)
}