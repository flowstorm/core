package org.promethist.core.repository

import org.litote.kmongo.Id
import org.promethist.common.query.Query
import org.promethist.core.model.Entity

interface EntityRepository<E : Entity<E>> {
    fun getAll(): List<E>
    fun get(id: Id<E>): E?
    fun find(query: Query): List<E>
    fun create(entity: E): E
    fun update(entity: E, upsert: Boolean = false): E

    class EntityNotFound(message: String) : Throwable(message)
}