package com.promethist.core.type

import com.promethist.core.Defaults
import java.time.ZoneId

class TimeString(override var value: String, override val zoneId: ZoneId = Defaults.zoneId) : TimeValue<String>(value, zoneId)