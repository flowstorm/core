package com.promethist.core.type

import com.promethist.core.Defaults
import java.math.BigDecimal
import java.time.ZoneId

class TimeBigDecimal(override var value : BigDecimal, override val zoneId: ZoneId = Defaults.zoneId) : TimeValue<BigDecimal>(value, zoneId)