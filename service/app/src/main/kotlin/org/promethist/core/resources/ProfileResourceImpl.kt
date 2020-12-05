package org.promethist.core.resources

import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import org.promethist.common.query.MongoFiltersFactory
import org.promethist.common.query.Query
import org.promethist.core.model.Profile
import org.promethist.core.model.User
import org.litote.kmongo.*
import javax.inject.Inject
import javax.ws.rs.NotFoundException
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import kotlin.collections.toList

@Path("/profiles")
@Produces(MediaType.APPLICATION_JSON)
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