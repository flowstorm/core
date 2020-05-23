package com.promethist.core.type

import com.fasterxml.jackson.annotation.JsonIgnore

data class Location(val longitude: Float = Float.MIN_VALUE, val latitude: Float = Float.MIN_VALUE) {
    @get:JsonIgnore
    val isEmpty get() = (longitude == Float.MIN_VALUE && latitude == Float.MIN_VALUE)
    @get:JsonIgnore
    val isNotEmpty get() = !isEmpty
}