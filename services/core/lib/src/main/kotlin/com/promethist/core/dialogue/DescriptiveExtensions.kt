package com.promethist.core.dialogue

import com.promethist.core.type.DateTime
import com.promethist.core.type.Location
import com.promethist.core.type.Memory
import kotlin.math.abs
import kotlin.math.roundToInt

fun BasicDialogue.describe(data: Map<String, Any>): String {
    val list = mutableListOf<String>()
    val isWord = when (language) {
        "en" -> "is"
        "de" -> "ist"
        "cs" -> "je"
        else -> unsupportedLanguage()
    }
    data.forEach {
        list.add("${it.key} $isWord " + describe(it.value))
    }
    return enumerate(list)
}

fun BasicDialogue.describe(data: Collection<String>) = enumerate(data)

fun BasicDialogue.describe(data: Memory<*>) = describe(data.value) + indent(describe(data.time, BasicDialogue.HIGH))

fun BasicDialogue.describe(data: Location) =
        if (data.isEmpty || data.latitude == null || data.longitude == null) {
            when (language) {
                "en" -> "unknown"
                "de" -> "unbekannt"
                "cs" -> "neznámá"
                else -> unsupportedLanguage()
            }
        } else {
            try {
                var latSec = (data.latitude!! * 3600).roundToInt()
                val latDeg = latSec / 3600
                latSec = Math.abs(latSec % 3600)
                val latMin = latSec / 60
                latSec %= 60

                var lngSec = (data.longitude!! * 3600).roundToInt()
                val lngDeg = lngSec / 3600
                lngSec = Math.abs(lngSec % 3600)
                val lngMin = lngSec / 60
                lngSec %= 60

                val latDir = if (latDeg >= 0) "N" else "S"
                val lonDir = if (lngDeg >= 0) "E" else "W"

                """${abs(latDeg)}° $latMin' $latSec" $latDir, ${abs(lngDeg)}° $lngMin' $lngSec" $lonDir"""
            } catch (e: Exception) {
                String.format("%8.5f", data.latitude) + ", " + String.format("%8.5f", data.longitude)
            }
        }

fun BasicDialogue.describe(data: Any?, detail: Int = 0) =
        when (data) {
            is Location -> describe(data)
            is DateTime -> if (data.isDate) describeDate(data, detail) else describeTime(data, detail)
            is String -> data
            null -> "undefined"
            else -> data.toString()
        }