package com.promethist.core.type

import java.time.ZonedDateTime

class TimeInt(override var value : Int, override val time: ZonedDateTime = ZonedDateTime.now()) : TimeValue<Int>(value, time)