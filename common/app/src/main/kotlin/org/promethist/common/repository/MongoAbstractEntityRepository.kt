
package org.promethist.common.repository

import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.UpdateOptions
import org.bson.conversions.Bson
import org.litote.kmongo.*
import org.litote.kmongo.id.ObjectIdGenerator
import org.promethist.common.model.Entity
import org.promethist.common.model.TimeEntity
import org.promethist.common.query.MongoFiltersFactory
import org.promethist.common.query.Query
import javax.inject.Inject
import kotlin.collections.toList
import kotlin.reflect.KProperty
import kotlin.reflect.full.memberProperties

abstract class MongoAbstractEntityRepository<E : Entity<*>> : EntityRepository<E> {

    @Inject
    lateinit var database: MongoDatabase

    protected abstract val collection: MongoCollection<E>

    override fun all() = collection.find().toList()

    override fun find(id: Id<E>): E? = collection.findOneById(id)

    override fun create(entity: E): E {
        collection.insertOne(entity)
        return entity
    }

    override fun update(entity: E, upsert: Boolean): E {
        collection.updateOneById(entity._id, entity, if (upsert) upsert() else UpdateOptions())
        return entity
    }

    override fun remove(id: Id<E>) {
        collection.deleteOneById(id)
    }

    fun updateById(id:Id<E>, entity: E): E {
        collection.updateOneById(id, entity)
        return entity
    }

    /**
     * Default seek without additional filters
     */
    protected inline fun <reified T : Entity<E>> seekFind(query: Query) =
        seek<T>(query).let { collection.aggregate(it).toList() }

    protected inline fun <reified T : Entity<E>> seek(
        query: Query,
        additionalFilters: List<Bson> = listOf()
    ): List<Bson> =
        mutableListOf<Bson>().apply {
            add(match(*additionalFilters.toTypedArray()))
            add(match(*MongoFiltersFactory.createFilters(T::class, query).toTypedArray()))
            val seekProperty = T::class.memberProperties.first { it.name == "_id" }
            add(sort(orderBy(seekProperty as KProperty<*>)))
            add(limit(query.limit))
        }

    protected inline fun <reified DTE : TimeEntity<E>> timeSeek(
        query: Query,
        additionalFilters: List<Bson> = listOf()
    ) = mutableListOf<Bson>().apply {
        add(match(*additionalFilters.toTypedArray()))
        query.seek_id?.let { seekId ->
            val seekDate = (collection.findOneById(ObjectIdGenerator.create(seekId))!! as DTE).datetime
            add(
                match(
                    or(
                        TimeEntity<E>::datetime lt seekDate,
                        and(
                            TimeEntity<E>::datetime eq seekDate,
                            TimeEntity<E>::_id lt ObjectIdGenerator.create(seekId)
                        )
                    )
                )
            )
        }
        add(sort(descending(TimeEntity<E>::datetime, TimeEntity<E>::_id)))
        add(match(*MongoFiltersFactory.createFilters(DTE::class, query, includeSeek = false).toTypedArray()))
        add(limit(query.limit))
    }
}