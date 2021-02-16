@file:Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")

package org.promethist.core.repository.mongo

import com.mongodb.client.model.UpdateOptions
import org.litote.kmongo.*
import org.promethist.common.query.MongoFiltersFactory
import org.promethist.common.query.Query
import org.promethist.core.model.Profile
import org.promethist.core.model.Space
import org.promethist.core.model.User
import org.promethist.core.repository.EntityRepository
import org.promethist.core.repository.ProfileRepository
import kotlin.collections.toList

class MongoProfileRepository : MongoAbstractEntityRepository<Profile>(), ProfileRepository {

    private val profiles by lazy { database.getCollection<Profile>() }

    override fun find(id: Id<Profile>): Profile? = profiles.findOneById(id)
    override fun find(query: Query): List<Profile> =
        profiles.aggregate(MongoFiltersFactory.createPipeline(Profile::class, query)).toList()

    override fun getAll(): List<Profile> = profiles.find().toList()
    override fun get(id: Id<Profile>): Profile = find(id) ?: throw EntityRepository.EntityNotFound("Profile $id not found")

    override fun create(profile: Profile): Profile {
        profiles.insertOne(profile)
        return profile
    }

    override fun update(profile: Profile, upsert: Boolean): Profile {
        profiles.updateOneById(profile._id, profile, if (upsert) upsert() else UpdateOptions())
        return profile
    }

    override fun findBy(userId: Id<User>, spaceId: Id<Space>): Profile? {
        return profiles.find(Profile::user_id eq userId, Profile::space_id eq spaceId)
            .apply { if (count() > 1) error("Multiple profiles found.") }
            .singleOrNull()
    }
}