package com.promethist.core.type

import java.time.ZonedDateTime

class TimeString(override var value: String, override val time: ZonedDateTime = ZonedDateTime.now()) : TimeValue<String>(value, time)