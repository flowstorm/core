package ai.flowstorm.core.repository

import org.litote.kmongo.Id
import ai.flowstorm.common.repository.EntityRepository
import ai.flowstorm.core.model.Profile
import ai.flowstorm.core.model.Space
import ai.flowstorm.core.model.User

interface ProfileRepository : EntityRepository<Profile> {
    fun findBy(userId: Id<User>, spaceId: Id<Space>): Profile?
}