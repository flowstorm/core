package com.promethist.core.resources

import com.promethist.core.model.Session
import com.promethist.core.model.User
import org.litote.kmongo.Id

interface SessionResource {
    fun create(session: Session)
    fun get(sessionId: String): Session?
    fun getForUser(userId: Id<User>): List<Session>
    fun update(session: Session)
}