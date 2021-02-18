@file:Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")

package org.promethist.core.repository.mongo

import org.litote.kmongo.*
import org.promethist.common.query.Query
import org.promethist.common.repository.MongoAbstractEntityRepository
import org.promethist.core.model.DialogueEvent
import org.promethist.core.repository.EventRepository
import kotlin.collections.toList

class MongoEventRepository : MongoAbstractEntityRepository<DialogueEvent>(), EventRepository {

    override val collection by lazy { database.getCollection<DialogueEvent>() }

    override fun find(query: Query) = timeSeek<DialogueEvent>(query).let { collection.aggregate(it).toList() }
}