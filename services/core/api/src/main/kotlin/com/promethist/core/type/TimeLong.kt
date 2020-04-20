package com.promethist.core.type

import com.promethist.core.Defaults
import java.time.ZoneId

class TimeLong(override var value : Long, override val zoneId: ZoneId = Defaults.zoneId) : TimeValue<Long>(value, zoneId)