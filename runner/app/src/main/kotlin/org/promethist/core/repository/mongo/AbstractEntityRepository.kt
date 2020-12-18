package org.promethist.core.repository.mongo

import com.mongodb.client.MongoDatabase
import org.promethist.core.model.Entity
import org.promethist.core.repository.EntityRepository
import javax.inject.Inject

abstract class AbstractEntityRepository<E: Entity<E>> : EntityRepository<E> {

    @Inject
    lateinit var database: MongoDatabase
}