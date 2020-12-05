package org.promethist.core.model

import org.litote.kmongo.Id
import org.litote.kmongo.newId

open class Device(
        open var _id: Id<Device> = newId(),
        open var deviceId: String,
        open var description: String
)