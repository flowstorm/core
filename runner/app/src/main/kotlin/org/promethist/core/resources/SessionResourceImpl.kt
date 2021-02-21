package org.promethist.core.resources

import org.promethist.common.query.Query
import org.promethist.common.security.Authorized
import org.promethist.core.model.Session
import org.promethist.core.model.Turn
import org.promethist.core.repository.SessionRepository
import org.promethist.core.repository.TurnRepository
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
    lateinit var turnRepository: TurnRepository

    @Inject
    lateinit var query: Query

    override fun find(): List<Session> = sessionRepository.find(query).onEach { load(it) }

    override fun create(session: Session) = sessionRepository.create(session)

    override fun create(turn: Turn) = turnRepository.create(turn)

    override fun update(session: Session) = session.run {
        turns = emptyList()
        sessionRepository.update(session, true)
    }

    override fun findBy(sessionId: String): Session? = sessionRepository.findBy(sessionId)?.let { load(it) }

    private fun load(session: Session) = session.apply {
        if (turns.isEmpty()) {
            turns = turnRepository.findBy(session._id)
        }
    }
}