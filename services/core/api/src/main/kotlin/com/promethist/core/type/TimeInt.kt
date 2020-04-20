package com.promethist.core.type

import com.promethist.core.Defaults
import java.time.ZoneId

class TimeInt(override var value : Int, override val zoneId: ZoneId = Defaults.zoneId) : TimeValue<Int>(value, zoneId)