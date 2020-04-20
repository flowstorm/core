package com.promethist.core.type

import com.promethist.core.Defaults
import java.time.ZoneId

class TimeFloat(override var value : Float, override val zoneId: ZoneId = Defaults.zoneId) : TimeValue<Float>(value, zoneId)