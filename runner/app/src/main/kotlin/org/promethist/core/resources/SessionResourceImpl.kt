package org.promethist.core.resources

import com.mongodb.client.MongoDatabase
import org.bson.conversions.Bson
import org.litote.kmongo.*
import org.litote.kmongo.id.ObjectIdGenerator
import org.promethist.common.query.MongoFiltersFactory
import org.promethist.common.query.Query
import org.promethist.common.security.Authorized
import org.promethist.core.model.Session
import org.promethist.core.model.User
import org.promethist.core.repository.SessionRepository
import javax.inject.Inject
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("/sessions")
@Produces(MediaType.APPLICATION_JSON)
@Authorized
class SessionResourceImpl: SessionResource {

    @Inject
    lateinit var sessionRepository: SessionRepository

    @Inject
    lateinit var query: Query

    override fun getSessions(): List<Session> = sessionRepository.getSessions(query)

    override fun create(session: Session) {
        sessionRepository.create(session)
    }

    override fun update(session: Session) {
        sessionRepository.update(session)
    }

    override fun get(sessionId: String): Session? = sessionRepository.get(sessionId)

    override fun getForUser(userId: Id<User>): List<Session> = sessionRepository.getForUser(userId)
}