package ai.flowstorm.core.repository

import org.litote.kmongo.Id
import ai.flowstorm.common.repository.EntityRepository
import ai.flowstorm.core.model.Session
import ai.flowstorm.core.model.Turn

interface TurnRepository : EntityRepository<Turn> {
    fun findBy(session_id: Id<Session>): List<Turn>
    fun findBy(session_ids: List<Id<Session>>): List<Turn>
}