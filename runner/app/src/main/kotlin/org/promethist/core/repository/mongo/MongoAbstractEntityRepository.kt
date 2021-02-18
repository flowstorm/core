package org.promethist.core.repository.mongo

import com.mongodb.client.MongoDatabase
import org.promethist.common.model.Entity
import org.promethist.core.repository.EntityRepository
import javax.inject.Inject

abstract class MongoAbstractEntityRepository<E: Entity<E>> : EntityRepository<E> {

    @Inject
    lateinit var database: MongoDatabase
}