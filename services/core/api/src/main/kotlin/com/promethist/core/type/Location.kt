package com.promethist.core.type

import com.fasterxml.jackson.annotation.JsonIgnore

data class Location(val longitude: Double = Double.MIN_VALUE, val latitude: Double = Double.MIN_VALUE) {

    @get:JsonIgnore
    val isEmpty get() = (longitude == Double.MIN_VALUE && latitude == Double.MIN_VALUE)
    @get:JsonIgnore
    val isNotEmpty get() = !isEmpty

    override fun toString() = "$longitude,$latitude"
}