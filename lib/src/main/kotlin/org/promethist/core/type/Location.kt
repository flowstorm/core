package org.promethist.core.type

import com.fasterxml.jackson.annotation.JsonIgnore

open class Location(
        var latitude: Double? = Double.MIN_VALUE, // lat
        var longitude: Double? = Double.MIN_VALUE, // lng
        var accuracy: Double? = null, // acc
        var altitude: Double? = null, // alt
        var altitudeAccuracy: Double? = null, // alt_acc
        var speed: Double? = null, // spd
        var speedAccuracy: Double? = null, // spd_acc
        var heading: Double? = null, // hdg
        var headingAccuracy: Double? = null // hdg_acc
) {
    override fun toString() =
            StringBuilder("lat=$latitude,lng=$longitude").apply {
                accuracy?.let { append(",acc=$it") }
                altitude?.let { append(",alt=$it") }
                altitudeAccuracy?.let { append(",alt_acc=$it") }
                speed?.let { append(",spd=$it") }
                speedAccuracy?.let { append(",spd_acc=$it") }
                heading?.let { append(",hdg=$it") }
                headingAccuracy?.let { append(",hdg_acc=$it") }
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