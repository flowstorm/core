package org.promethist.core.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import org.promethist.core.type.Attributes
import org.litote.kmongo.Id
import org.litote.kmongo.newId

data class Community (
        val _id: Id<Community> = newId(),
        val name: String,
        var space_id: String?,  //var only for BC
        val attributes: Attributes = Attributes()
) {
    @Deprecated("Renamed to space_id")
    var organization_id: String?
        @JsonProperty
        set(value) {
            if (space_id == null) space_id = value
        }
        @JsonIgnore
        get() = space_id

}