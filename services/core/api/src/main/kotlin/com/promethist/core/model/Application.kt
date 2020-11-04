package com.promethist.core.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.promethist.common.ServiceUrlResolver
import com.promethist.core.type.MutablePropertyMap
import org.litote.kmongo.Id
import org.litote.kmongo.newId

open class Application(
        open var _id: Id<Application> = newId(),
        open var name: String,
        open var dialogue_id: Id<DialogueModel>? = null,
        open var public: Boolean = false,
        open var icon: String? = null,
        open var properties: MutablePropertyMap = mutableMapOf()
) {
    @get:JsonIgnore
    val link get() = ServiceUrlResolver.getEndpointUrl("bot", domain = "promethist.ai") + "/" + _id
}