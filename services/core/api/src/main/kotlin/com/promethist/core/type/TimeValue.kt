package com.promethist.core.type

import com.fasterxml.jackson.annotation.JsonIgnore
import java.time.Instant
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.*

open class TimeValue<V>(override var value: V, open val zoneId: ZoneId, private val timestamp: Long = System.currentTimeMillis()): Value<V>(value) {

    @get:JsonIgnore
    val time: ZonedDateTime
        get() = ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp), TimeZone.getDefault().toZoneId())
}