package ai.flowstorm.core.model

import org.litote.kmongo.Id
import org.litote.kmongo.newId
import ai.flowstorm.core.type.Attributes

data class Community (
        val _id: Id<Community> = newId(),
        val name: String,
        var space_id: String, //TODO convert to Id<Space>
        val attributes: Attributes = Attributes()
)