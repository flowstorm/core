@file:Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")

package ai.flowstorm.core.repository.mongo

import org.litote.kmongo.*
import ai.flowstorm.common.query.Query
import ai.flowstorm.common.repository.MongoAbstractEntityRepository
import ai.flowstorm.core.model.Profile
import ai.flowstorm.core.model.Space
import ai.flowstorm.core.model.User
import ai.flowstorm.core.repository.ProfileRepository
import kotlin.collections.toList

class MongoProfileRepository : MongoAbstractEntityRepository<Profile>(), ProfileRepository {

    override val collection by lazy { database.getCollection<Profile>() }

    override fun find(query: Query) = seek<Profile>(query).let { collection.aggregate(it).toList() }

    override fun findBy(userId: Id<User>, spaceId: Id<Space>): Profile? {
        return collection.find(Profile::user_id eq userId, Profile::space_id eq spaceId)
            .apply { if (count() > 1) error("Multiple profiles found.") }
            .singleOrNull()
    }
}