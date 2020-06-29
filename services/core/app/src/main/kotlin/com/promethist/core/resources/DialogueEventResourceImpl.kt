package com.promethist.core.resources

import com.mongodb.client.MongoDatabase
import com.promethist.common.query.MongoFiltersFactory
import com.promethist.common.query.Query
import com.promethist.core.model.DialogueEvent
import com.promethist.core.model.Session
import org.litote.kmongo.Id
import org.litote.kmongo.eq
import org.litote.kmongo.getCollection
import org.litote.kmongo.toList
import javax.inject.Inject

class DialogueEventResourceImpl: DialogueEventResource {

    @Inject
    lateinit var database: MongoDatabase

    @Inject
    lateinit var query: Query

    private val dialogueEvents by lazy { database.getCollection<DialogueEvent>() }


    override fun getDialogueEvents(): List<DialogueEvent> {
        return dialogueEvents.aggregate(MongoFiltersFactory.createPipeline(DialogueEvent::class, query)).toList()
    }

    override fun create(dialogueEvent: DialogueEvent) {
        dialogueEvents.insertOne(dialogueEvent)
    }

    override fun get(eventId: Id<DialogueEvent>): DialogueEvent? {
        return dialogueEvents.find(DialogueEvent::_id eq eventId).singleOrNull()
    }

    override fun getForSession(sessionId: Id<Session>): List<DialogueEvent> {
        return dialogueEvents.find(DialogueEvent::sessionId  eq sessionId).toMutableList()
    }



}