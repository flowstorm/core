package ai.flowstorm.core.model

import org.litote.kmongo.Id
import ai.flowstorm.common.model.Entity

interface Space : Entity<Space> {
    override val _id: Id<Space>
    val name: String
}