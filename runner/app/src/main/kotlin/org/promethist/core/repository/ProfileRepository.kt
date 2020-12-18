package org.promethist.core.repository

import org.promethist.core.model.Profile
import org.promethist.core.model.User
import org.litote.kmongo.Id
import org.promethist.core.model.Space

interface ProfileRepository : EntityRepository<Profile> {
    fun findBy(userId: Id<User>, spaceId: Id<Space>): Profile?
}