package com.promethist.core.model

import com.promethist.core.type.Dynamic
import org.litote.kmongo.Id
import org.litote.kmongo.newId

data class Profile(
        val _id: Id<Profile> = newId(),
        val user_id: Id<User>,
        val attributes: Dynamic = Dynamic()
)