package com.promethist.core.model

import com.promethist.core.type.Dynamic
import org.litote.kmongo.Id
import org.litote.kmongo.newId

data class Profile(var _id: Id<Application> = newId(), var name: String? = null, var properties: Dynamic = Dynamic())