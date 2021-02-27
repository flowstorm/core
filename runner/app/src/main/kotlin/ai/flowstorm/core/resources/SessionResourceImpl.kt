package ai.flowstorm.core.resources

import ai.flowstorm.common.query.Query
import ai.flowstorm.common.security.Authorized
import ai.flowstorm.core.model.Session
import ai.flowstorm.core.model.Turn
import ai.flowstorm.core.repository.SessionRepository
import ai.flowstorm.core.repository.TurnRepository
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

    override fun find(): List<Session> = sessionRepository.find(query).apply { load(this) }

    override fun create(session: Session) = sessionRepository.create(session)

    override fun create(turn: Turn) = turnRepository.create(turn)

    override fun update(session: Session) = session.copy().run {
        turns = emptyList()
        sessionRepository.update(this, true)
        session
    }

    override fun findBy(sessionId: String): Session? = sessionRepository.findBy(sessionId)?.let { load(it) }

    private fun load(sessions: List<Session>) = sessions.filter { it.turns.isEmpty() }.let { sessions ->
        val turns = turnRepository.findBy(sessions.map { it._id })
        sessions.forEach { s ->
            s.turns = turns.filter { turn -> turn.session_id == s._id }
        }
    }

    private fun load(session: Session) = session.apply {
        if (turns.isEmpty()) {
            turns = turnRepository.findBy(session._id)
        }
    }
}