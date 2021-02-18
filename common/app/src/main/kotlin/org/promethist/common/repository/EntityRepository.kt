package org.promethist.common.repository

import org.litote.kmongo.Id
import org.promethist.common.model.Entity
import org.promethist.common.query.Query

interface EntityRepository<E : Entity<*>> {
    fun all(): List<E>
    fun get(id: Id<E>): E = find(id) ?:  throw EntityNotFound("Entity $id not found in repository ${this::class.simpleName}")
    fun find(id: Id<E>): E?
    fun find(query: Query): List<E>
    fun create(entity: E): E
    fun update(entity: E, upsert: Boolean = false): E
    fun remove(id:Id<E>)

    class EntityNotFound(message: String) : Throwable(message)
}