package com.promethist.core.profile

import com.mongodb.client.MongoDatabase
import com.promethist.core.model.Profile
import com.promethist.core.model.User
import org.litote.kmongo.*
import javax.inject.Inject

class MongoProfileRepository : ProfileRepository {

    @Inject
    lateinit var database: MongoDatabase

    override fun find(userId: Id<User>): Profile? {
        return database.getCollection<Profile>().find(Profile::user_id eq userId)
                .apply { if (count() > 1) error("Multiple profiles found.") }
                .singleOrNull()
    }

    override fun save(profile: Profile) {
        database.getCollection<Profile>().updateOneById(profile._id, profile, upsert())
    }
}