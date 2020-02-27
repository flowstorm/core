package com.promethistai.core.model

import org.bson.types.ObjectId

data class Profile(var _id: ObjectId? = null, var name: String? = null, var properties: MutableMap<String, Any> = mutableMapOf())