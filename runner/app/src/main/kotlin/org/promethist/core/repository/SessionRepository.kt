package org.promethist.core.repository

import org.litote.kmongo.Id
import org.promethist.common.query.Query
import org.promethist.core.model.Session
import org.promethist.core.model.User

interface SessionRepository : EntityRepository<Session> {
    fun getSessions(query: Query): List<Session>
    fun getForUser(userId: Id<User>): List<Session>
    fun get(sessionId: String): Session?
}