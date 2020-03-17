package com.promethist.core.type

import com.fasterxml.jackson.annotation.JsonIgnore
import java.time.Instant
import java.time.LocalDateTime
import java.util.*

open class TimeValue<V>(override var value: V, private val timestamp: Long = System.currentTimeMillis()): Value<V>(value) {

    @get:JsonIgnore
    val time: LocalDateTime
        get() = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), TimeZone.getDefault().toZoneId())
}