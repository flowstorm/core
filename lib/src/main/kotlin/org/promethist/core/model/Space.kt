package org.promethist.core.model

import org.litote.kmongo.Id
import org.promethist.common.model.Entity

interface Space : Entity<Space> {
    override val _id: Id<Space>
    val name: String
}