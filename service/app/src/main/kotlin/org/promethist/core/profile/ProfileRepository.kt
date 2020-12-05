package org.promethist.core.profile

import org.promethist.core.model.Profile
import org.promethist.core.model.User
import org.litote.kmongo.Id

interface ProfileRepository {
    fun find(userId: Id<User>): Profile?
    fun save(profile: Profile)
}