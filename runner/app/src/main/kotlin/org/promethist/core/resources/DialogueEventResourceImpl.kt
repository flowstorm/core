package org.promethist.core.resources

import com.mongodb.client.MongoDatabase
import org.bson.conversions.Bson
import org.litote.kmongo.*
import org.litote.kmongo.id.ObjectIdGenerator
import org.promethist.common.query.MongoFiltersFactory
import org.promethist.common.query.Query
import org.promethist.core.model.DialogueEvent
import org.promethist.core.model.Session
import javax.inject.Inject
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("/dialogueEvent")
@Produces(MediaType.APPLICATION_JSON)
class DialogueEventResourceImpl: DialogueEventResource {

    @Inject
    lateinit var database: MongoDatabase

    @Inject
    lateinit var query: Query

    private val dialogueEvents by lazy { database.getCollection<DialogueEvent>() }

    override fun getDialogueEvents(): List<DialogueEvent> {
        val pipeline: MutableList<Bson> = mutableListOf()
        pipeline.apply {
            query.seek_id?.let { seekId ->
                val seekDate = dialogueEvents.findOneById(ObjectIdGenerator.create(seekId))!!.datetime
                add(match(or(
                        DialogueEvent::datetime lt seekDate,
                        and(
                                DialogueEvent::datetime eq seekDate,
                                DialogueEvent::_id lt ObjectIdGenerator.create(seekId)
                        )
                )))
            }

            add(sort(descending(DialogueEvent::datetime, DialogueEvent::_id)))
            add(match(*MongoFiltersFactory.createFilters(Session::class, query, includeSeek = false).toTypedArray()))

            add(limit(query.limit))
        }

        return dialogueEvents.aggregate(pipeline).toMutableList()
    }

    override fun create(dialogueEvent: DialogueEvent) {
        dialogueEvents.insertOne(dialogueEvent)
    }

    override fun get(eventId: Id<DialogueEvent>): DialogueEvent? {
        return dialogueEvents.find(DialogueEvent::_id eq eventId).singleOrNull()
    }
}