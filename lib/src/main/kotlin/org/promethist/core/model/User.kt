package org.promethist.core.model

import org.litote.kmongo.Id
import org.litote.kmongo.newId
import org.promethist.common.model.Entity
import org.promethist.security.Identity
import java.util.*

open class User(
    override var _id: Id<User> = newId(),
    override var username: String,
    override var name: String,
    override var surname: String,
    override var nickname: String,
    open var agreement: Date? = null
) : Identity(username, name, surname, nickname), Entity<User>
