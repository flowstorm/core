package com.promethistai.core.model

import org.litote.kmongo.Id
import org.litote.kmongo.newId

open class User(open var _id: Id<User> = newId(),  open var username: String, open var name: String, open var surname: String, open var nickname: String)
