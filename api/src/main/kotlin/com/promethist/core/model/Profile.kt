package com.promethist.core.model

import com.promethist.core.type.Attributes
import org.litote.kmongo.Id
import org.litote.kmongo.newId

data class Profile(
        override val _id: Id<Profile> = newId(),
        val user_id: Id<User>,
        val attributes: Attributes = Attributes()
) : Entity<Profile>