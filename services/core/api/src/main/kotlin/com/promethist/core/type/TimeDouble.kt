package com.promethist.core.type

import java.time.ZonedDateTime

class TimeDouble(override var value : Double, override val time: ZonedDateTime = ZonedDateTime.now()) : TimeValue<Double>(value, time)