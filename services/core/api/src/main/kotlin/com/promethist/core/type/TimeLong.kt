package com.promethist.core.type

import java.time.ZonedDateTime

class TimeLong(override var value : Long, override val time: ZonedDateTime = ZonedDateTime.now()) : TimeValue<Long>(value, time)