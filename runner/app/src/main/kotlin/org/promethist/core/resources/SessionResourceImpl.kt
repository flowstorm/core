package org.promethist.core.resources

import org.litote.kmongo.*
import org.promethist.common.resources.EntityResourceBase
import org.promethist.common.security.Authorized
import org.promethist.core.model.Session
import org.promethist.core.model.User
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("/sessions")
@Produces(MediaType.APPLICATION_JSON)
@Authorized
class SessionResourceImpl: SessionResource, EntityResourceBase<Session>() {

    override val collection by lazy { database.getCollection<Session>() }

    override fun list() = collection.aggregate(seek<Session>()).toMutableList()

    override fun create(session: Session) {
        collection.insertOne(session)
    }

    override fun update(session: Session) {
        collection.updateOneById(session._id, session, upsert())
    }

    override fun getForId(sessionId: String): Session? {
        return collection.find(Session::sessionId eq sessionId).singleOrNull()
    }

    override fun getForUser(userId: Id<User>): List<Session> {
        return collection.find(Session::user / User::_id eq userId).toMutableList()
    }
}