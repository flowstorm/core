package com.promethist.core.profile

import com.promethist.core.model.Profile
import com.promethist.core.model.User
import org.litote.kmongo.Id

interface ProfileRepository {
    fun find(userId: Id<User>): Profile?
    fun save(profile: Profile)
}