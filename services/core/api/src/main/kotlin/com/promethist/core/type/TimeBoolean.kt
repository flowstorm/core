package com.promethist.core.type

import com.promethist.core.Defaults
import java.time.ZoneId

class TimeBoolean(override var value : Boolean, override val zoneId: ZoneId = Defaults.zoneId) : TimeValue<Boolean>(value, zoneId)