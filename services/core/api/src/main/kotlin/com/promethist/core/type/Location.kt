package com.promethist.core.type

data class Location(val longitude: Float = Float.MIN_VALUE, val latitude: Float = Float.MIN_VALUE) {
    fun isEmpty() = (longitude == Float.MIN_VALUE && latitude == Float.MIN_VALUE)
    fun isNotEmpty() = !isEmpty()
}