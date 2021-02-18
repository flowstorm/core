@file:Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")

package org.promethist.core.repository.mongo

import org.litote.kmongo.*
import org.promethist.common.query.Query
import org.promethist.common.repository.MongoAbstractEntityRepository
import org.promethist.core.model.Profile
import org.promethist.core.model.Space
import org.promethist.core.model.User
import org.promethist.core.repository.ProfileRepository
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