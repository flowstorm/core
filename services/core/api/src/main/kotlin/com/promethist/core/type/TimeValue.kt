package com.promethist.core.type

import com.fasterxml.jackson.annotation.JsonIgnore
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.*

open class TimeValue<V>(override var value: V, private val timestamp: Long = System.currentTimeMillis()): Value<V>(value) {

    @get:JsonIgnore
    val time: LocalDateTime
        get() = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), TimeZone.getDefault().toZoneId())

    fun wasDay(from: Long, to: Long = from): Boolean {
        var fromDay = LocalDateTime.now().minus(from, ChronoUnit.DAYS)
        var toDay = LocalDateTime.now().minus(to, ChronoUnit.DAYS)
        if (fromDay.isAfter(toDay)) {
            val day = fromDay
            fromDay = toDay
            toDay = day
        }
        val fromTime = fromDay.with(LocalTime.of(0, 0, 0))
        val toTime = toDay.with(LocalTime.of(23, 59, 59))
        val time = time
        return time.isEqual(fromTime) || time.isEqual(toTime) || (time.isAfter(fromTime) && time.isBefore(toTime))
    }

    fun isToday() = wasDay(0, 0)
    fun wasYesterday() = wasDay(1, 1)

}