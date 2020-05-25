package com.promethist.core.type

import com.promethist.core.Defaults
import java.time.ZonedDateTime

typealias DateTime = ZonedDateTime

const val ZERO_TEMPERATURE = -273.15
val ZERO_TIME = DateTime.of(0, 1, 1, 0, 0, 0, 0, Defaults.zoneId)