package com.promethist.core.type

import java.math.BigDecimal
import java.time.ZonedDateTime

class TimeBigDecimal(override var value : BigDecimal, override val time: ZonedDateTime = ZonedDateTime.now()) : TimeValue<BigDecimal>(value, time)