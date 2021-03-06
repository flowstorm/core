package ai.flowstorm.core.repository.mongo

import org.litote.kmongo.Id
import org.litote.kmongo.`in`
import org.litote.kmongo.eq
import org.litote.kmongo.getCollection
import ai.flowstorm.common.query.Query
import ai.flowstorm.common.repository.MongoAbstractEntityRepository
import ai.flowstorm.core.model.Session
import ai.flowstorm.core.model.Turn
import ai.flowstorm.core.repository.TurnRepository

class MongoTurnRepository : MongoAbstractEntityRepository<Turn>(), TurnRepository {

    override val collection by lazy { database.getCollection<Turn>() }

    override fun find(query: Query): List<Turn> = timeSeek<Turn>(query).let { collection.aggregate(it).toList() }

    override fun findBy(session_id: Id<Session>): List<Turn> = collection.find(Turn::session_id eq session_id).toList()

    override fun findBy(session_ids: List<Id<Session>>): List<Turn> = collection.find(Turn::session_id `in` session_ids).toList()
}