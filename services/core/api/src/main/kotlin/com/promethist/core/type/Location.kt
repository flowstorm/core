package com.promethist.core.type

import com.fasterxml.jackson.annotation.JsonIgnore

data class Location(
        val latitude: Double = Double.MIN_VALUE, // lat
        val longitude: Double = Double.MIN_VALUE, // lng
        val accuracy: Double? = null, // acc
        val altitude: Double? = null, // alt
        val altitudeAccuracy: Double? = null, // alt_acc
        val speed: Double? = null, // spd
        val speedAccuracy: Double? = null, // spd_acc
        val heading: Double? = null, // hdg
        val headingAccuracy: Double? = null // hdg_acc
) {
    override fun toString() =
            StringBuilder("lat=$latitude,lng=$longitude").apply {
                accuracy?.let { append("acc=$it") }
                altitude?.let { append("alt=$it") }
                altitudeAccuracy?.let { append("alt_acc=$it") }
                speed?.let { append("spd=$it") }
                speedAccuracy?.let { append("spd_acc=$it") }
                heading?.let { append("hdg=$it") }
                headingAccuracy?.let { append("hdg_acc=$it") }
            }.toString()

    @get:JsonIgnore
    val isEmpty get() = (latitude == Double.MIN_VALUE && longitude == Double.MIN_VALUE)
    @get:JsonIgnore
    val isNotEmpty get() = !isEmpty
}

fun String.toLocation() =
        mutableMapOf<String, Double>().run {
            split(',').forEach { s ->
                s.split('=').let { p ->
                    put(p[0], p[1].toDouble())
                }
            }
            Location(get("lat")!!, get("lng")!!, get("acc"), get("alt"), get("alt_acc"), get("spd"), get("spd_acc"), get("hdg"), get("hdg_acc"))
        }