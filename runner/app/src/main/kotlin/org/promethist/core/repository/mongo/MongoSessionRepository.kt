package org.promethist.core.repository.mongo

import org.litote.kmongo.*
import org.promethist.common.query.Query
import org.promethist.common.repository.MongoAbstractEntityRepository
import org.promethist.core.model.Session
import org.promethist.core.model.User
import org.promethist.core.repository.SessionRepository
import kotlin.collections.toList

class MongoSessionRepository:  MongoAbstractEntityRepository<Session>(), SessionRepository {

    override val collection by lazy { database.getCollection<Session>() }

    override fun findBy(sessionId: String): Session? = collection.find(Session::sessionId eq sessionId).singleOrNull()

    override fun find(query: Query): List<Session> = timeSeek<Session>(query).let { collection.aggregate(it).toList() }

    override fun findBy(userId: Id<User>): List<Session> = collection.find(Session::user / User::_id eq userId).toList()
}