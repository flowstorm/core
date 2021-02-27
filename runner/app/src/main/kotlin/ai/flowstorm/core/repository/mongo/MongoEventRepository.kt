@file:Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")

package ai.flowstorm.core.repository.mongo

import org.litote.kmongo.*
import ai.flowstorm.common.query.Query
import ai.flowstorm.common.repository.MongoAbstractEntityRepository
import ai.flowstorm.core.model.DialogueEvent
import ai.flowstorm.core.repository.EventRepository
import kotlin.collections.toList

class MongoEventRepository : MongoAbstractEntityRepository<DialogueEvent>(), EventRepository {

    override val collection by lazy { database.getCollection<DialogueEvent>() }

    override fun find(query: Query) = timeSeek<DialogueEvent>(query).let { collection.aggregate(it).toList() }
}