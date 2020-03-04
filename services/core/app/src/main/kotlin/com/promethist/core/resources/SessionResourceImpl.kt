package com.promethist.core.resources

import com.mongodb.client.MongoDatabase
import com.promethist.core.model.Session
import com.promethist.core.model.User
import org.litote.kmongo.*
import javax.inject.Inject

class SessionResourceImpl: SessionResource {

    @Inject
    lateinit var database: MongoDatabase

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