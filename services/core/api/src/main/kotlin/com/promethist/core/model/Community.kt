package com.promethist.core.model

import com.promethist.core.type.Attributes
import org.litote.kmongo.Id
import org.litote.kmongo.newId

data class Community(
        val _id: Id<Community> = newId(),
        val name: String,
        val attributes: Attributes = Attributes()
)