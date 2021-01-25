@file:Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")

package org.promethist.core.repository.mongo

import com.mongodb.client.model.UpdateOptions
import org.bson.conversions.Bson
import org.litote.kmongo.*
import org.litote.kmongo.id.ObjectIdGenerator
import org.promethist.common.query.MongoFiltersFactory
import org.promethist.common.query.Query
import org.promethist.core.model.DialogueEvent
import org.promethist.core.model.Session
import org.promethist.core.repository.EventRepository
import kotlin.collections.toList

class MongoEventRepository : MongoAbstractEntityRepository<DialogueEvent>(), EventRepository {

    private val dialogueEventCollection by lazy { database.getCollection<DialogueEvent>() }

    override fun get(id: Id<DialogueEvent>): DialogueEvent? = dialogueEventCollection.findOneById(id)
    override fun find(query: Query): List<DialogueEvent> =
        dialogueEventCollection.aggregate(MongoFiltersFactory.createPipeline(DialogueEvent::class, query)).toList()

    override fun getDialogueEvents(query: Query): List<DialogueEvent> {
        val pipeline: MutableList<Bson> = mutableListOf()
        pipeline.apply {
            query.seek_id?.let { seekId ->
                val seekDate = dialogueEventCollection.findOneById(ObjectIdGenerator.create(seekId))!!.datetime
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

        return dialogueEventCollection.aggregate(pipeline).toMutableList()
    }

    override fun getAll(): List<DialogueEvent> = dialogueEventCollection.find().toList()

    override fun create(dialogueEvent: DialogueEvent): DialogueEvent {
        dialogueEventCollection.insertOne(dialogueEvent)
        return dialogueEvent
    }

    override fun update(dialogueEvent: DialogueEvent, upsert: Boolean): DialogueEvent {
        dialogueEventCollection.updateOneById(dialogueEvent._id, dialogueEvent, if (upsert) upsert() else UpdateOptions())
        return dialogueEvent
    }

}