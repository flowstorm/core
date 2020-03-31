package com.promethist.port.model

import com.promethist.core.model.User
import org.litote.kmongo.Id
import org.litote.kmongo.newId
import java.util.*

data class Connection(var _id: Id<Connection> = newId(), val sender: String, val created: Date, val user_id: Id<User>? = null)