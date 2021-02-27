package ai.flowstorm.core.model

import org.litote.kmongo.Id
import org.litote.kmongo.newId
import ai.flowstorm.core.type.Attributes
import ai.flowstorm.common.model.Entity

data class Profile(
        override val _id: Id<Profile> = newId(),
        val user_id: Id<User>,
        val space_id: Id<Space>,
        val attributes: Attributes = Attributes()
) : Entity<Profile>