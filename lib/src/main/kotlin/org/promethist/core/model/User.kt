package org.promethist.core.model

import org.promethist.security.Identity
import org.litote.kmongo.Id
import org.litote.kmongo.newId
import java.util.*

open class User(
    open var _id: Id<User> = newId(),
    override var username: String,
    override var name: String,
    override var surname: String,
    override var nickname: String,
    open var agreement: Date? = null
) : Identity(username, name, surname, nickname)
