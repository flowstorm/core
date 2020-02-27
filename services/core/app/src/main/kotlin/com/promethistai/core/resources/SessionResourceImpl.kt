package com.promethistai.core.resources

import com.mongodb.client.MongoDatabase
import com.promethistai.admin.model.Session
import com.promethistai.admin.model.User
import com.promethistai.admin.resources.SessionResource
import org.litote.kmongo.Id
import org.litote.kmongo.eq
import org.litote.kmongo.getCollection
import org.litote.kmongo.updateOneById
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
        return database.getCollection<Session>().find(Session::user_id eq userId).toMutableList()
    }
}