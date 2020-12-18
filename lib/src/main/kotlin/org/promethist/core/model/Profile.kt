package org.promethist.core.model

import org.litote.kmongo.Id
import org.litote.kmongo.newId
import org.promethist.core.type.Attributes

data class Profile(
        override val _id: Id<Profile> = newId(),
        val user_id: Id<User>,
        val space_id: Id<Space>,
        val attributes: Attributes = Attributes()
) : Entity<Profile>