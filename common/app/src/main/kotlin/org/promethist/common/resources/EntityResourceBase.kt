package org.promethist.common.resources

import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import org.bson.conversions.Bson
import org.litote.kmongo.*
import org.litote.kmongo.id.ObjectIdGenerator
import org.promethist.common.query.MongoFiltersFactory
import org.promethist.common.query.Query
import org.promethist.common.model.TimeEntity
import org.promethist.common.model.Entity
import javax.inject.Inject
import kotlin.collections.toList

abstract class EntityResourceBase<E : Entity<*>> {

    @Inject
    lateinit var database: MongoDatabase

    @Inject
    lateinit var query: Query

    abstract val collection: MongoCollection<E>

    inline fun <reified DTE : TimeEntity<E>> seek(filters: MutableList<Bson>? = null) = mutableListOf<Bson>().apply {
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
        add(match(*(filters ?: MongoFiltersFactory.createFilters(DTE::class, query, includeSeek = false)).toTypedArray()))
        add(limit(query.limit))
    }

    open fun list() = collection.find().toList()

    open fun get(id: Id<E>) = collection.find(Entity<E>::_id eq id).singleOrNull()

}