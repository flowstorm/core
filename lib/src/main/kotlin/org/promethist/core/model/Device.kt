package org.promethist.core.model

import org.litote.kmongo.Id
import org.litote.kmongo.newId
import org.promethist.common.model.Entity

open class Device(
        override var _id: Id<Device> = newId(),
        open var deviceId: String,
        open var description: String
) : Entity<Device>