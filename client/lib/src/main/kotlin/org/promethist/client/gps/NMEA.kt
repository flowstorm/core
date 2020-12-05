package org.promethist.client.gps

import org.promethist.core.type.Location
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.net.Socket

object NMEA {

    internal interface Parser {
        fun parse(tokens: Array<String>, location: GPSLocation): Boolean
    }

    internal class GPGGA : Parser {
        override fun parse(tokens: Array<String>, location: GPSLocation): Boolean {
            location.time = tokens[1].toDouble()
            location.latitude = parseLatitude(tokens[2], tokens[3])
            location.longitude = parseLongitude(tokens[4], tokens[5])
            location.quality = tokens[6].toInt()
            location.altitude = tokens[9].ifEmpty { null }?.toDouble()
            return true
        }
    }

    internal class GPGGL : Parser {
        override fun parse(tokens: Array<String>, location: GPSLocation): Boolean {
            location.latitude = parseLatitude(tokens[1], tokens[2])
            location.longitude = parseLongitude(tokens[3], tokens[4])
            location.time = tokens[5].toDouble()
            return true
        }
    }

    internal class GPRMC : Parser {
        override fun parse(tokens: Array<String>, location: GPSLocation): Boolean {
            location.time = tokens[1].toDouble()
            location.latitude = parseLatitude(tokens[3], tokens[4])
            location.longitude = parseLongitude(tokens[5], tokens[6])
            location.speed = tokens[7].ifEmpty { null }?.toDouble()
            location.heading = tokens[8].ifEmpty { null }?.toDouble()
            return true
        }
    }

    internal class GPVTG : Parser {
        override fun parse(tokens: Array<String>, location: GPSLocation): Boolean {
            location.heading = tokens[3].ifEmpty { null }?.toDouble()
            return true
        }
    }

    internal class GPRMZ : Parser {
        override fun parse(tokens: Array<String>, location: GPSLocation): Boolean {
            location.altitude = tokens[1].toDouble()
            return true
        }
    }

    class GPSLocation : Location() {
        var time = 0.0
        var fixed = false
        var quality = 0

        fun updatefix() {
            fixed = quality > 0
        }

        override fun toString() = super.toString() + ",time=$time,fixed=$fixed,quality=$quality"
    }

    val location = GPSLocation()
    private val parsers = mapOf(
            "GPGGA" to GPGGA(),
            "GPGGL" to GPGGL(),
            "GPRMC" to GPRMC(),
            "GPRMZ" to GPRMZ(),
            "GPVTG" to GPVTG()
    )

    fun parse(line: String): Location {
        if (line.startsWith("$")) {
            val nmea = line.substring(1)
            val tokens = nmea.split(",").toTypedArray()
            val type = tokens[0]
            //TODO check crc
            if (parsers.containsKey(type)) {
                parsers[type]!!.parse(tokens, location)
            }
            location.updatefix()
        }
        return location
    }

    fun parseLatitude(lat: String, NS: String): Double? {
        if (lat.isBlank())
            return null
        var med = lat.substring(2).toDouble() / 60.0
        med += lat.substring(0, 2).toDouble()
        if (NS.startsWith("S")) {
            med = -med
        }
        return med
    }

    fun parseLongitude(lon: String, WE: String): Double? {
        if (lon.isBlank())
            return null
        var med = lon.substring(3).toDouble() / 60.0
        med += lon.substring(0, 3).toDouble()
        if (WE.startsWith("W")) {
            med = -med
        }
        return med
    }

    @JvmStatic
    fun main(args: Array<String>) = test(args[0])

    fun test(path: String) {
        (if (path.startsWith('/'))
            FileInputStream(File(path))
        else
            path.split(':').let {
                Socket(it[0], it[1].toInt()).getInputStream()
            }
        ).use {
            val reader = BufferedReader(InputStreamReader(it))
            while (true) {
                val line = reader.readLine() ?: break
                parse(line)
                println("$location\t\t$line")
            }
        }
    }
}