package org.promethist.common.repository

import com.amazonaws.services.dynamodbv2.document.*
import org.litote.kmongo.Id
import org.promethist.common.AppConfig
import org.promethist.common.ObjectUtil
import org.promethist.common.model.Entity
import javax.inject.Inject

abstract class DynamoAbstractEntityRepository<E: Entity<E>> : EntityRepository<E> {

    @Inject
    lateinit var database: DynamoDB

    protected abstract val table: Table

    protected fun tableName(name: String) = AppConfig.instance["name"] + "." + name

    override fun create(entity: E): E {
        table.putItem(Item.fromJSON(ObjectUtil.defaultMapper.writeValueAsString(entity)))
        return entity
    }

    override fun update(entity: E, upsert: Boolean): E {
        return create(entity) // it will be updated if the id is the same
    }

    override fun remove(id: Id<E>) {
        table.deleteItem(KeyAttribute("_id", id.toString()))
    }

    inline fun <reified T: E> Item.toEntity(): T = ObjectUtil.defaultMapper.readValue(this.toJSON(), T::class.java)

    inline fun <reified T: E> ItemCollection<*>.toEntityList(): List<T> = map { ObjectUtil.defaultMapper.readValue(it.toJSON(), T::class.java) }
}