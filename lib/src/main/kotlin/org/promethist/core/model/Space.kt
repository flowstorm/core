package org.promethist.core.model

import org.litote.kmongo.Id

interface Space {
    val _id: Id<Space>
    val name: String
}