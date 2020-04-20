package com.promethist.core.type

import com.promethist.core.Defaults
import java.time.ZoneId

class TimeDouble(override var value : Double, override val zoneId: ZoneId = Defaults.zoneId) : TimeValue<Double>(value, zoneId)