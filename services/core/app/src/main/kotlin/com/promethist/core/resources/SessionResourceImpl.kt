package com.promethist.core.resources

import com.mongodb.client.MongoDatabase
import com.promethist.common.query.MongoFiltersFactory
import com.promethist.common.query.Query
import com.promethist.common.query.QueryParams
import com.promethist.core.model.Session
import com.promethist.core.model.User
import org.bson.conversions.Bson
import org.litote.kmongo.*
import org.litote.kmongo.id.ObjectIdGenerator
import javax.inject.Inject

class SessionResourceImpl: SessionResource {

    @Inject
    lateinit var database: MongoDatabase

    @QueryParams
    lateinit var query:Query

    override fun getSessions(): List<Session> {
        val pipeline: MutableList<Bson> = mutableListOf()
        pipeline.apply {
            query.seek_id?.let { seekId ->
                val seekDate = database.getCollection<Session>().findOneById(ObjectIdGenerator.create(seekId))!!.datetime
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

        val result = database.getCollection<Session>().aggregate(pipeline).toMutableList()
        return result
    }

    override fun create(session: Session) {
        database.getCollection<Session>().insertOne(session)
    }

    override fun update(session: Session) {
        database.getCollection<Session>().updateOneById(session._id,  session)
    }

    override fun get(sessionId: String): Session? {
        return database.getCollection<Session>().find(Session::sessionId eq sessionId).singleOrNull()
    }

    override fun getForUser(userId: Id<User>): List<Session> {
        return database.getCollection<Session>().find(Session::user / User::_id eq userId).toMutableList()
    }
}