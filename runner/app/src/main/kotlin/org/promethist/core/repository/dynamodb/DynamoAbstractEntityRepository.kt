package org.promethist.core.repository.dynamodb

import com.amazonaws.services.dynamodbv2.document.DynamoDB
import org.promethist.common.AppConfig
import org.promethist.core.model.Entity
import org.promethist.core.repository.EntityRepository
import javax.inject.Inject

abstract class DynamoAbstractEntityRepository<E: Entity<E>> : EntityRepository<E> {

    @Inject
    lateinit var database: DynamoDB

    protected fun tableName(name: String) = AppConfig.instance["name"] + "." + name
}