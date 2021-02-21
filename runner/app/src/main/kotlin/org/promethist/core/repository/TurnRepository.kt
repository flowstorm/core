package org.promethist.core.repository

import org.litote.kmongo.Id
import org.promethist.common.repository.EntityRepository
import org.promethist.core.model.Session
import org.promethist.core.model.Turn

interface TurnRepository : EntityRepository<Turn> {
    fun findBy(session_id: Id<Session>): List<Turn>
}