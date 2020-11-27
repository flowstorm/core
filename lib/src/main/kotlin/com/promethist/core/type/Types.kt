package com.promethist.core.type

import com.promethist.core.Defaults
import java.math.BigDecimal
import java.time.ZonedDateTime

typealias DateTime = ZonedDateTime
typealias Decimal = BigDecimal
typealias Random = kotlin.random.Random

const val ZERO_TEMPERATURE = -273.15
val ZERO_TIME = DateTime.of(0, 1, 1, 0, 0, 0, 0, Defaults.zoneId)
val DEFAULT_LOCATION = Location(50.101, 14.395)