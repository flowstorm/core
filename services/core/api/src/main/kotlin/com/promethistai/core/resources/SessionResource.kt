package com.promethistai.core.resources

import com.promethistai.admin.model.Session
import com.promethistai.admin.model.User
import org.litote.kmongo.Id

interface SessionResource {
    fun create(session: Session)
    fun get(sessionId: String): Session?
    fun getForUser(userId: Id<User>): List<Session>
    fun update(session: Session)
}