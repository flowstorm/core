package org.promethist.core.model

import org.litote.kmongo.Id
import org.litote.kmongo.newId

data class SpaceImpl(
        override val _id: Id<Space> = newId(),
        override val name: String
) : Space