package com.promethist.core.model

import org.litote.kmongo.Id
import org.litote.kmongo.newId

data class Profile(var _id: Id<Application> = newId(), var name: String? = null, var properties: MutableMap<String, Any> = mutableMapOf())