package org.promethist.core.repository.dynamodb

import com.amazonaws.services.dynamodbv2.document.DynamoDB
import org.litote.kmongo.Id
import org.promethist.common.AppConfig
import org.promethist.common.model.Entity
import org.promethist.common.repository.EntityRepository
import javax.inject.Inject

abstract class DynamoAbstractEntityRepository<E: Entity<E>> : EntityRepository<E> {

    @Inject
    lateinit var database: DynamoDB

    override fun remove(id: Id<E>) {
        TODO("Not yet implemented")
    }

    companion object {
        fun tableName(name: String) = AppConfig.instance["name"] + "." + name
    }
}