package com.promethist.core.type

import java.time.ZonedDateTime

class TimeFloat(override var value : Float, override val time: ZonedDateTime = ZonedDateTime.now()) : TimeValue<Float>(value, time)