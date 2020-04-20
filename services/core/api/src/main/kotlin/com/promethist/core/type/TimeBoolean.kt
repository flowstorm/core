package com.promethist.core.type

import java.time.ZonedDateTime

class TimeBoolean(override var value : Boolean, override val time: ZonedDateTime = ZonedDateTime.now()) : TimeValue<Boolean>(value, time)