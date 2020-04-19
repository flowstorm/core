package com.promethist.core.resources

import com.mongodb.client.MongoDatabase
import com.promethist.common.query.MongoFiltersFactory
import com.promethist.common.query.Query
import com.promethist.core.model.Profile
import org.litote.kmongo.*
import javax.inject.Inject
import javax.ws.rs.NotFoundException
import kotlin.collections.toList

class ProfileResourceImpl : ProfileResource {

    @Inject
    lateinit var database: MongoDatabase

    @Inject
    lateinit var query: Query

    private val profiles get() = database.getCollection<Profile>()

    override fun getProfiles(): List<Profile> {
        return profiles.aggregate(MongoFiltersFactory.createPipeline(Profile::class, query)).toList()
    }

    override fun get(profileId: Id<Profile>): Profile {
        return profiles.findOneById(profileId) ?: throw NotFoundException("Profile $profileId not found.")
    }

    override fun create(profile: Profile) {
        profiles.insertOne(profile)
    }

    override fun update(profileId: Id<Profile>, profile: Profile) {
        profiles.updateOneById(profileId, profile)
    }
}