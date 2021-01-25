package org.promethist.core.repository.mongo

import org.bson.conversions.Bson
import org.litote.kmongo.*
import org.litote.kmongo.id.ObjectIdGenerator
import org.promethist.common.query.MongoFiltersFactory
import org.promethist.common.query.Query
import org.promethist.core.model.Session
import org.promethist.core.model.User
import org.promethist.core.repository.SessionRepository

class MongoSessionRepository:  MongoAbstractEntityRepository<Session>(), SessionRepository {

    private val sessions by lazy { database.getCollection<Session>() }

    override fun getSessions(query: Query): List<Session> {
        return find(query)
    }

    override fun create(session: Session): Session {
        sessions.insertOne(session)
        return session
    }

    override fun update(entity: Session, upsert: Boolean): Session {
        sessions.updateOneById(entity._id, entity, upsert())
        return entity
    }

    override fun get(sessionId: String): Session? {
        return sessions.find(Session::sessionId eq sessionId).singleOrNull()
    }

    override fun getAll(): List<Session> {
        return sessions.find().toMutableList()
    }

    override fun get(id: Id<Session>): Session? {
        return sessions.find(Session::_id eq id).singleOrNull()
    }

    override fun find(query: Query): List<Session> {
        val pipeline: MutableList<Bson> = mutableListOf()
        pipeline.apply {
            query.seek_id?.let { seekId ->
                val seekDate = sessions.findOneById(ObjectIdGenerator.create(seekId))!!.datetime
                add(match(or(
                    Session::datetime lt seekDate,
                    and(
                        Session::datetime eq seekDate,
                        Session::_id lt ObjectIdGenerator.create(seekId)
                    )
                )))
            }

            add(sort(descending(Session::datetime, Session::_id)))
            add(match(*MongoFiltersFactory.createFilters(Session::class, query, includeSeek = false).toTypedArray()))
            add(limit(query.limit))
        }

        return sessions.aggregate(pipeline).toMutableList()
    }

    override fun getForUser(userId: Id<User>): List<Session> {
        return sessions.find(Session::user / User::_id eq userId).toMutableList()
    }
}