package ai.flowstorm.core.repository

import org.litote.kmongo.Id
import ai.flowstorm.common.query.Query
import ai.flowstorm.common.repository.EntityRepository
import ai.flowstorm.core.model.Session
import ai.flowstorm.core.model.User

interface SessionRepository : EntityRepository<Session> {
    fun findBy(userId: Id<User>): List<Session>
    fun findBy(sessionId: String): Session?
}