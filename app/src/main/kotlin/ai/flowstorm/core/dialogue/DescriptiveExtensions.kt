package ai.flowstorm.core.dialogue

import ai.flowstorm.core.type.DateTime
import ai.flowstorm.core.type.Location
import ai.flowstorm.core.type.Memory
import java.time.Duration
import kotlin.math.abs
import kotlin.math.absoluteValue
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
            null -> when (language) {
                "de" -> "undefiniert"
                "cs" -> "nedefinované"
                else -> "undefined"
            }
            else -> data.toString()
        }

fun BasicDialogue.describeDate(data: DateTime = now, detail: Int = 0): String {
    val delta = Duration.between(data, now)
    if (data.toLocalDate() == today.toLocalDate()) { // events that took/take place in days around the current date
        return mapOf(
            "en" to "today",
            "de" to "heute",
            "cs" to "dnes",
            "fr" to "aujourd'hui"
        )[language] ?: unsupportedLanguage()
    } else if (data.toLocalDate() == yesterday.toLocalDate()) {
        return mapOf( //TODO: days in different languages
            "en" to "yesterday",
            "de" to "gestern",
            "cs" to "včera",
            "fr" to "heir"
        )[language] ?: unsupportedLanguage()
    } else if (data.toLocalDate() == tomorrow.toLocalDate()) {
        return mapOf( //TODO: days in different languages
            "en" to "tomorrow",
            "de" to "morgen",
            "cs" to "zítra",
            "fr" to "demain"
        )[language] ?: unsupportedLanguage()
    } else if (delta.seconds.absoluteValue < 604800) { //TODO Gender of days in CZ, day name localization
        return mapOf( //TODO: days in different languages
            "en" to (if (!delta.isNegative) "last" else "next") + " ${data.dayOfWeekName}",
            "de" to (if (!delta.isNegative) "letzten" else "nächsten") + " ${data.dayOfWeekName}",
            "cs" to (if (!delta.isNegative) "minulou" else "příští") + " ${data.dayOfWeekName}",
            "fr" to "${data.dayOfWeekName} " + (if (!delta.isNegative) "dernier" else "prochain")
        )[language] ?: unsupportedLanguage()
    } else if (delta.seconds.absoluteValue < 864000) { // events with longer time difference
        when {
            detail == 0 && !delta.isNegative -> {
                return mapOf(
                    "en" to "a week ago",
                    "de" to "vor einer Woche",
                    "cs" to "před týdnem",
                    "fr" to "il y a une semaine"
                )[language] ?: unsupportedLanguage()
            }
            detail == 0 -> {
                return mapOf(
                    "en" to "in a week",
                    "de" to "in einer Woche",
                    "cs" to "za týden",
                    "fr" to "dans une semaine"
                )[language] ?: unsupportedLanguage()
            }
            else -> {
                return dateInMonth(data)
            }
        }
    } else if (delta.seconds.absoluteValue < 1382400) {
        when {
            detail == 0 && !delta.isNegative -> {
                return mapOf(
                    "en" to "two weeks ago",
                    "de" to "vor zwei Wochen",
                    "cs" to "před dvěma týdny",
                    "fr" to "il y a deux semaines"
                )[language] ?: unsupportedLanguage()
            }
            detail == 0 -> {
                return mapOf(
                    "en" to "in two week",
                    "de" to "in zwei Wochen",
                    "cs" to "za dva týdny",
                    "fr" to "en deux semaines"
                )[language] ?: unsupportedLanguage()
            }
            else -> {
                return dateInMonth(data)
            }
        }
    } else if (delta.seconds.absoluteValue < 2678400 && data.monthValue == now.monthValue) {
        when {
            detail == 0 && !delta.isNegative -> {
                return mapOf(
                    "en" to "a few weeks ago",
                    "de" to "vor ein paar Wochen",
                    "cs" to "před pár týdny",
                    "fr" to "il y a quelques semaines"
                )[language] ?: unsupportedLanguage()
            }
            detail == 0 -> {
                return mapOf(
                    "en" to "in a few weeks",
                    "de" to "in ein paar Wochen",
                    "cs" to "za pár týdnů",
                    "fr" to "dans quelques semaines"
                )[language] ?: unsupportedLanguage()
            }
            else -> {
                return dateInMonth(data)
            }
        }
    } else if (delta.seconds.absoluteValue < 5356800 && (data.monthValue == now.monthValue - 1 || (data.monthValue == 12 && now.monthValue == 1))) {
        return if (detail == 0) {
            mapOf(
                "en" to "last month",
                "de" to "Im vergangenen Monat",
                "cs" to "minulý měsíc",
                "fr" to "le mois dernier"
            )[language] ?: unsupportedLanguage()
        } else {
            dateInMonth(data)
        }
    } else if (delta.seconds.absoluteValue < 5356800 && (data.monthValue == now.monthValue + 1 || (data.monthValue == 1 && now.monthValue == 12))) {
        return if (detail == 0) {
            mapOf(
                "en" to "next month",
                "de" to "nächsten Monat",
                "cs" to "příští měsíc",
                "fr" to "le mois prochain"
            )[language] ?: unsupportedLanguage()
        } else {
            dateInMonth(data)
        }
    } else if (data.year == now.year) {
        return if (detail < 2) {
            mapOf(
                "en" to "in ${data.monthName}",
                "de" to "im ${data.monthName}",
                "cs" to "v ${data.monthName}",
                "fr" to "en ${data.monthName}"
            )[language] ?: unsupportedLanguage()
        } else {
            dateInMonth(data)
        }
    } else if (data.year == now.year - 1) {
        return mapOf(
            "en" to "last year" + (if (detail == 1) " in ${data.monthName}" else if (detail > 1) ", ${dateInMonth(data)}" else ""),
            "de" to "letztes Jahr" + (if (detail == 1) " im ${data.monthName}" else if (detail > 1) ", ${dateInMonth(data)}" else ""),
            "cs" to "minulý rok" + (if (detail == 1) " v ${data.monthName}" else if (detail > 1) ", ${dateInMonth(data)}" else ""),
            "fr" to "l'année dernière" + (if (detail == 1) " en ${data.monthName}" else if (detail > 1) ", ${dateInMonth(data)}" else "")
        )[language] ?: unsupportedLanguage()
    } else if (data.year == now.year + 1) {
        return mapOf(
            "en" to "next year" + (if (detail == 1) " in ${data.monthName}" else if (detail > 1) ", ${dateInMonth(data)}" else ""),
            "de" to "nächstes Jahr" + (if (detail == 1) " im ${data.monthName}" else if (detail > 1) ", ${dateInMonth(data)}" else ""),
            "cs" to "příští rok" + (if (detail == 1) " v ${data.monthName}" else if (detail > 1) ", ${dateInMonth(data)}" else ""),
            "fr" to "l'année prochaine" + (if (detail == 1) " en ${data.monthName}" else if (detail > 1) ", ${dateInMonth(data)}" else "")
        )[language] ?: unsupportedLanguage()
    } else {
        return mapOf(
            "en" to (if (detail == 0) "in" else if (detail == 1) "in ${data.monthName}" else if (detail > 1) dateInMonth(data) else "") + " ${data.year}",
            "de" to (if (detail == 0) "in" else if (detail == 1) "im ${data.monthName}" else if (detail > 1) dateInMonth(data) else "") + " ${data.year}",
            "cs" to (if (detail == 0) "v roce" else if (detail == 1) "v ${data.monthName}" else if (detail > 1) dateInMonth(data) else "") + " ${data.year}",
            "fr" to (if (detail == 0) "en" else if (detail == 1) "en ${data.monthName}" else if (detail > 1) dateInMonth(data) else "") + " ${data.year}"
        )[language] ?: unsupportedLanguage()
    }
}

fun BasicDialogue.describeTime(data: DateTime = now, detail: Int = 0): String {
    val delta = Duration.between(data, now)
    if (delta.seconds.absoluteValue < 60) {  // time related events up to few hours ago/in the future
        if (!delta.isNegative) {
            when {
                detail == 3 -> {
                    return mapOf(
                        "en" to "${delta.seconds.absoluteValue} seconds ago",
                        "de" to "vor ${delta.seconds.absoluteValue} Sekunden",
                        "cs" to "před ${delta.seconds.absoluteValue} sekundami",
                        "fr" to "il ya ${delta.seconds.absoluteValue} secondes"
                    )[language] ?: unsupportedLanguage()
                }
                detail > 0 -> {
                    return mapOf(
                        "en" to "${(delta.seconds.absoluteValue + 5) / 5 * 5} seconds ago",
                        "de" to "vor ${(delta.seconds.absoluteValue + 5) / 5 * 5} Sekunden",
                        "cs" to "před ${(delta.seconds.absoluteValue + 5) / 5 * 5} sekundami",
                        "fr" to "il ya ${(delta.seconds.absoluteValue + 5) / 5 * 5} secondes"
                    )[language] ?: unsupportedLanguage()
                }
                else -> {
                    return mapOf(
                        "en" to "a few seconds ago",
                        "de" to "vor ein paar Sekunden",
                        "cs" to "před pár sekundami",
                        "fr" to "il ya quelques secondes"
                    )[language] ?: unsupportedLanguage()
                }
            }
        } else {
            when {
                detail == 3 -> {
                    return mapOf(
                        "en" to "in ${delta.seconds.absoluteValue} seconds",
                        "de" to "in ${delta.seconds.absoluteValue} Sekunden",
                        "cs" to "za ${delta.seconds.absoluteValue} sekund",
                        "fr" to "en ${delta.seconds.absoluteValue} secondes"
                    )[language] ?: unsupportedLanguage()
                }
                detail > 0 -> {
                    return mapOf(
                        "en" to "in ${(delta.seconds.absoluteValue + 5) / 10 * 10} seconds",
                        "de" to "in ${(delta.seconds.absoluteValue + 5) / 10 * 10} Sekunden",
                        "cs" to "za ${(delta.seconds.absoluteValue + 5) / 10 * 10} sekund",
                        "fr" to "en ${(delta.seconds.absoluteValue + 5) / 10 * 10} secondes"
                    )[language] ?: unsupportedLanguage()
                }
                else -> {
                    return mapOf(
                        "en" to "in a few seconds",
                        "de" to "in wenigen Sekunden",
                        "cs" to "za pár sekund",
                        "fr" to "en quelques secondes"
                    )[language] ?: unsupportedLanguage()
                }
            }
        }
    } else if (delta.seconds.absoluteValue < 120) {
        if (!delta.isNegative) {
            when {
                detail == 3 && delta.seconds.absoluteValue > 60 -> {
                    return mapOf( //TODO localize plural of datetime units
                        "en" to "a minute and ${(delta.seconds.absoluteValue - 60) of "second"} ago",
                        "de" to "vor einer Minute und ${delta.seconds.absoluteValue - 60} Sekunden",
                        "cs" to "před minutou a ${delta.seconds.absoluteValue - 60} sekundami",
                        "fr" to "il ya une minute et ${delta.seconds.absoluteValue - 60} secondes"
                    )[language] ?: unsupportedLanguage()
                }
                detail > 0 && delta.seconds.absoluteValue > 65 -> {
                    return mapOf(
                        "en" to "a minute and ${(delta.seconds.absoluteValue + 5 - 60) / 10 * 10} seconds ago",
                        "de" to "vor einer Minute und ${(delta.seconds.absoluteValue + 5 - 60) / 10 * 10} Sekunden",
                        "cs" to "před minutou a ${(delta.seconds.absoluteValue + 5 - 60) / 10 * 10} sekundami",
                        "fr" to "il ya une minute et ${(delta.seconds.absoluteValue + 5 - 60) / 10 * 10} secondes"
                    )[language] ?: unsupportedLanguage()
                }
                else -> {
                    return mapOf(
                        "en" to "a minute ago",
                        "de" to "vor einer Minute",
                        "cs" to "před minutou",
                        "fr" to "Il y'a une minute"
                    )[language] ?: unsupportedLanguage()
                }
            }
        } else {
            when {
                detail == 3 && delta.seconds.absoluteValue > 60 -> {
                    return mapOf( //TODO localize plural of datetime units
                        "en" to "in a minute and ${(delta.seconds.absoluteValue - 60) of "second"}",
                        "de" to "in einer Minute und ${delta.seconds.absoluteValue - 60} Sekunden",
                        "cs" to "za minutu a ${delta.seconds.absoluteValue - 60} sekund",
                        "fr" to "dans une minute et ${delta.seconds.absoluteValue - 60} secondes"
                    )[language] ?: unsupportedLanguage()
                }
                detail > 0 && delta.seconds.absoluteValue > 65 -> {
                    return mapOf(
                        "en" to "in a minute and ${(delta.seconds.absoluteValue + 5 - 60) / 10 * 10} seconds",
                        "de" to "in einer Minute und ${(delta.seconds.absoluteValue + 5 - 60) / 10 * 10} Sekunden",
                        "cs" to "za minutu a ${(delta.seconds.absoluteValue + 5 - 60) / 10 * 10} sekund",
                        "fr" to "dans une minute et ${(delta.seconds.absoluteValue + 5 - 60) / 10 * 10} secondes"
                    )[language] ?: unsupportedLanguage()
                }
                else -> {
                    return mapOf(
                        "en" to "in a minute",
                        "de" to "in einer Minute",
                        "cs" to "za minutu",
                        "fr" to "dans une minute"
                    )[language] ?: unsupportedLanguage()
                }
            }
        }
    } else if (delta.seconds.absoluteValue < 1200) {
        if (!delta.isNegative) {
            when {
                detail > 1 -> {
                    return mapOf(
                        "en" to "${delta.toMinutes().absoluteValue} minutes ago",
                        "de" to "vor ${delta.toMinutes().absoluteValue} Minuten",
                        "cs" to "před ${delta.toMinutes().absoluteValue} minutami",
                        "fr" to "il y a ${delta.toMinutes().absoluteValue} minutes"
                    )[language] ?: unsupportedLanguage()
                }
                detail == 1 -> {
                    return mapOf(
                        "en" to "${(delta.toMinutes().absoluteValue + 3) / 5 * 5} minutes ago",
                        "de" to "vor ${(delta.toMinutes().absoluteValue + 3) / 5 * 5} Minuten",
                        "cs" to "před ${(delta.toMinutes().absoluteValue + 3) / 5 * 5} minutami",
                        "fr" to "il y a ${(delta.toMinutes().absoluteValue + 3) / 5 * 5} minutes"
                    )[language] ?: unsupportedLanguage()
                }
                else -> {
                    return mapOf(
                        "en" to "a few minutes ago",
                        "de" to "vor ein paar Minuten",
                        "cs" to "před několika minutami",
                        "fr" to "il y a quelques minutes"
                    )[language] ?: unsupportedLanguage()
                }
            }
        } else {
            when {
                detail > 1 -> {
                    return mapOf(
                        "en" to "in ${delta.toMinutes().absoluteValue} minutes",
                        "de" to "in ${delta.toMinutes().absoluteValue} Minuten",
                        "cs" to "za ${delta.toMinutes().absoluteValue} minut",
                        "fr" to "dans ${delta.toMinutes().absoluteValue} minutes"
                    )[language] ?: unsupportedLanguage()
                }
                detail == 1 -> {
                    return mapOf(
                        "en" to "in ${(delta.toMinutes().absoluteValue + 3) / 5 * 5} minutes",
                        "de" to "in ${(delta.toMinutes().absoluteValue + 3) / 5 * 5} Minuten",
                        "cs" to "za ${(delta.toMinutes().absoluteValue + 3) / 5 * 5} minut",
                        "fr" to "dans ${(delta.toMinutes().absoluteValue + 3) / 5 * 5} minutes"
                    )[language] ?: unsupportedLanguage()
                }
                else -> {
                    return mapOf(
                        "en" to "in a few minutes",
                        "de" to "in ein paar Minuten",
                        "cs" to "za pár minut",
                        "fr" to "dans quelques minutes"
                    )[language] ?: unsupportedLanguage()
                }
            }
        }
    } else if (delta.seconds.absoluteValue < 2400) {
        if (!delta.isNegative) {
            when {
                detail > 2 -> {
                    return mapOf(
                        "en" to "${delta.toMinutes().absoluteValue} minutes ago",
                        "de" to "vor ${delta.toMinutes().absoluteValue} Minuten",
                        "cs" to "před ${delta.toMinutes().absoluteValue} minutami",
                        "fr" to "il y a ${delta.toMinutes().absoluteValue} minutes"
                    )[language] ?: unsupportedLanguage()
                }
                detail > 0 -> {
                    return mapOf(
                        "en" to "${(delta.toMinutes().absoluteValue + 3) / 5 * 5} minutes ago",
                        "de" to "vor ${(delta.toMinutes().absoluteValue + 3) / 5 * 5} Minuten",
                        "cs" to "před ${(delta.toMinutes().absoluteValue + 3) / 5 * 5} minutami",
                        "fr" to "il y a ${(delta.toMinutes().absoluteValue + 3) / 5 * 5} minutes"
                    )[language] ?: unsupportedLanguage()
                }
                else -> {
                    return mapOf(
                        "en" to "half an hour ago",
                        "de" to "vor einer halben Stunde",
                        "cs" to "před půl hodinou",
                        "fr" to "il y a une demi-heure"
                    )[language] ?: unsupportedLanguage()
                }
            }
        } else {
            when {
                detail > 2 -> {
                    return mapOf(
                        "en" to "in ${delta.toMinutes().absoluteValue} minutes",
                        "de" to "in ${delta.toMinutes().absoluteValue} Minuten",
                        "cs" to "za ${delta.toMinutes().absoluteValue} minut",
                        "fr" to "dans ${delta.toMinutes().absoluteValue} minutes"
                    )[language] ?: unsupportedLanguage()
                }
                detail > 0 -> {
                    return mapOf(
                        "en" to "in ${(delta.toMinutes().absoluteValue + 3) / 5 * 5} minutes",
                        "de" to "in ${(delta.toMinutes().absoluteValue + 3) / 5 * 5} Minuten",
                        "cs" to "za ${(delta.toMinutes().absoluteValue + 3) / 5 * 5} minut",
                        "fr" to "dans ${(delta.toMinutes().absoluteValue + 3) / 5 * 5} minutes"
                    )[language] ?: unsupportedLanguage()
                }
                else -> {
                    return mapOf(
                        "en" to "in half an hour",
                        "de" to "in einer halben Stunde",
                        "cs" to "za půl hodiny",
                        "fr" to "dans une demi-heure"
                    )[language] ?: unsupportedLanguage()
                }
            }
        }
    } else if (delta.seconds.absoluteValue < 5400) {
        if (!delta.isNegative) {
            when {
                detail > 2 && delta.seconds.absoluteValue < 3600 -> {
                    return mapOf(
                        "en" to "${delta.toMinutes().absoluteValue} minutes ago",
                        "de" to "vor ${delta.toMinutes().absoluteValue} Minuten",
                        "cs" to "před ${delta.toMinutes().absoluteValue} minutami",
                        "fr" to "il y a ${delta.toMinutes().absoluteValue} minutes"
                    )[language] ?: unsupportedLanguage()
                }
                detail > 2 && delta.seconds.absoluteValue > 3660 -> {
                    return mapOf( //TODO localize plural of datetime units
                        "en" to "an hour and ${(delta.toMinutes().absoluteValue - 60) of "minute"} ago",
                        "de" to "vor einer Stunde und ${delta.toMinutes().absoluteValue - 60} Minuten",
                        "cs" to "před hodinou a ${delta.toMinutes().absoluteValue - 60} minutami",
                        "fr" to "il y a une heure et ${delta.toMinutes().absoluteValue - 60} minutes"
                    )[language] ?: unsupportedLanguage()
                }
                detail > 0 && delta.seconds.absoluteValue < 3600 -> {
                    return mapOf(
                        "en" to "${(delta.toMinutes().absoluteValue + 2) / 5 * 5} minutes ago",
                        "de" to "vor ${(delta.toMinutes().absoluteValue + 2) / 5 * 5} Minuten",
                        "cs" to "před ${(delta.toMinutes().absoluteValue + 2) / 5 * 5} minutami",
                        "fr" to "il y a ${(delta.toMinutes().absoluteValue + 2) / 5 * 5} minutes"
                    )[language] ?: unsupportedLanguage()
                }
                detail > 0 && delta.seconds.absoluteValue > 3900 -> {
                    return mapOf(
                        "en" to "an hour and ${(delta.toMinutes().absoluteValue + 2 - 60) / 5 * 5} minutes ago",
                        "de" to "vor einer Stunde und ${(delta.toMinutes().absoluteValue + 2 - 60) / 5 * 5} Minuten",
                        "cs" to "před hodinou a ${(delta.toMinutes().absoluteValue + 2 - 60) / 5 * 5} minutami",
                        "fr" to "il y a une heure et ${(delta.toMinutes().absoluteValue + 2 - 60) / 5 * 5} minutes"
                    )[language] ?: unsupportedLanguage()
                }
                else -> {
                    return mapOf(
                        "en" to "an hour ago",
                        "de" to "vor einer Stunde",
                        "cs" to "před hodinou",
                        "fr" to "il y a une heure"
                    )[language] ?: unsupportedLanguage()
                }
            }
        } else {
            when {
                detail > 2 && delta.seconds.absoluteValue < 3600 -> {
                    return mapOf(
                        "en" to "in ${delta.toMinutes().absoluteValue} minutes",
                        "de" to "in ${delta.toMinutes().absoluteValue} Minuten",
                        "cs" to "za ${delta.toMinutes().absoluteValue} minut",
                        "fr" to "dans ${delta.toMinutes().absoluteValue} minutes"
                    )[language] ?: unsupportedLanguage()
                }
                detail > 2 && delta.seconds.absoluteValue > 3660 -> {
                    return mapOf( //TODO localize plural of datetime units
                        "en" to "in an hour and ${(delta.toMinutes().absoluteValue - 60) of "minute"}",
                        "de" to "in einer Stunde und ${delta.toMinutes().absoluteValue - 60} Minuten",
                        "cs" to "za hodinou a ${delta.toMinutes().absoluteValue - 60} minut",
                        "fr" to "dans une heure et ${delta.toMinutes().absoluteValue - 60} minutes"
                    )[language] ?: unsupportedLanguage()
                }
                detail > 0 && delta.seconds.absoluteValue < 3600 -> {
                    return mapOf(
                        "en" to "in ${(delta.toMinutes().absoluteValue + 2) / 5 * 5} minutes",
                        "de" to "in ${(delta.toMinutes().absoluteValue + 2) / 5 * 5} Minuten",
                        "cs" to "za ${(delta.toMinutes().absoluteValue + 2) / 5 * 5} minut",
                        "fr" to "dans ${(delta.toMinutes().absoluteValue + 2) / 5 * 5} minutes"
                    )[language] ?: unsupportedLanguage()
                }
                detail > 0 && delta.seconds.absoluteValue > 3900 -> {
                    return mapOf(
                        "en" to "in an hour and ${(delta.toMinutes().absoluteValue + 2 - 60) / 5 * 5} minutes",
                        "de" to "in einer Stunde und ${(delta.toMinutes().absoluteValue + 2 - 60) / 5 * 5} Minuten",
                        "cs" to "za hodinu a ${(delta.toMinutes().absoluteValue + 2 - 60) / 5 * 5} minut",
                        "fr" to "dans une heure et ${(delta.toMinutes().absoluteValue + 2 - 60) / 5 * 5} minutes"
                    )[language] ?: unsupportedLanguage()
                }
                else -> {
                    return mapOf(
                        "en" to "in an hour",
                        "de" to "in einer Stunde",
                        "cs" to "za hodinu",
                        "fr" to "dans une heure"
                    )[language] ?: unsupportedLanguage()
                }
            }
        }
    } else if (delta.seconds.absoluteValue < 10800) {
        if (!delta.isNegative) {
            when {
                detail > 2 && delta.seconds.absoluteValue < 7020 -> {
                    return mapOf(
                        "en" to "an hour and ${(delta.toMinutes().absoluteValue - 60) / 5 * 5} minutes ago",
                        "de" to "vor einer Stunde und ${(delta.toMinutes().absoluteValue - 60) / 5 * 5} Minuten",
                        "cs" to "před hodinou a ${(delta.toMinutes().absoluteValue - 60) / 5 * 5} minutami",
                        "fr" to "il y a une heure et ${(delta.toMinutes().absoluteValue - 60) / 5 * 5} minutes"
                    )[language] ?: unsupportedLanguage()
                }
                detail > 2 && delta.toMinutes().absoluteValue % 60 < 5 -> {
                    return mapOf(
                        "en" to "${((delta.plusMinutes(5)).toHours().absoluteValue)} hours ago",
                        "de" to "vor ${((delta.plusMinutes(5)).toHours().absoluteValue)} Stunden",
                        "cs" to "před ${((delta.plusMinutes(5)).toHours().absoluteValue)} hodinami",
                        "fr" to "il y a ${((delta.plusMinutes(5)).toHours().absoluteValue)} heures"
                    )[language] ?: unsupportedLanguage()
                }
                detail > 2 && delta.toMinutes().absoluteValue % 60 > 5 -> {
                    return mapOf(
                        "en" to "${((delta.abs().plusMinutes(5)).toHours())} hours and ${((delta.abs().toMinutes()) - ((delta.abs().plusMinutes(5)).toHours() * 60)) / 5 * 5} minutes ago",
                        "de" to "vor ${((delta.abs().plusMinutes(5)).toHours())} Stunden und ${((delta.abs().toMinutes()) - ((delta.abs().plusMinutes(5)).toHours() * 60)) / 5 * 5} Minuten",
                        "cs" to "před ${((delta.abs().plusMinutes(5)).toHours())} hodinami a ${((delta.abs().toMinutes()) - ((delta.abs().plusMinutes(5)).toHours() * 60)) / 5 * 5} minutami",
                        "fr" to "il y a ${((delta.abs().plusMinutes(5)).toHours())} heures and ${((delta.abs().toMinutes()) - ((delta.abs().plusMinutes(5)).toHours() * 60)) / 5 * 5} minutes"
                    )[language] ?: unsupportedLanguage()
                }
                detail > 0 -> {
                    return mapOf(
                        "en" to "${((delta.abs().plusMinutes(30)).toHours())} hours ago",
                        "de" to "vor ${((delta.abs().plusMinutes(30)).toHours())} Stunden",
                        "cs" to "před ${((delta.abs().plusMinutes(30)).toHours())} hodinami",
                        "fr" to "il y a ${((delta.abs().plusMinutes(30)).toHours())} heures"
                    )[language] ?: unsupportedLanguage()
                }
                else -> {
                    return mapOf(
                        "en" to "a few hours ago",
                        "de" to "vor ein paar Stunden",
                        "cs" to "před několika hodinami",
                        "fr" to "quelques heures plus tôt"
                    )[language] ?: unsupportedLanguage()
                }
            }
        } else {
            when {
                detail > 2 && delta.seconds.absoluteValue < 7020 -> {
                    return mapOf(
                        "en" to "in an hour and ${(delta.toMinutes().absoluteValue - 60) / 5 * 5} minutes",
                        "de" to "in einer Stunde und ${(delta.toMinutes().absoluteValue - 60) / 5 * 5} Minuten",
                        "cs" to "za hodinu a ${(delta.toMinutes().absoluteValue - 60) / 5 * 5} minut",
                        "fr" to "dans une heure et ${(delta.toMinutes().absoluteValue - 60) / 5 * 5} minutes"
                    )[language] ?: unsupportedLanguage()
                }
                detail > 2 && delta.toMinutes().absoluteValue % 60 < 5 -> {
                    return mapOf(
                        "en" to "in ${((delta.abs().plusMinutes(5)).toHours())} hours",
                        "de" to "in ${((delta.abs().plusMinutes(5)).toHours())} Stunden",
                        "cs" to "za ${((delta.abs().plusMinutes(5)).toHours())} hodiny",
                        "fr" to "dans ${((delta.abs().plusMinutes(5)).toHours())} heures"
                    )[language] ?: unsupportedLanguage()
                }
                detail > 2 && delta.toMinutes().absoluteValue % 60 > 5 -> {
                    return mapOf(
                        "en" to "in ${((delta.abs().plusMinutes(5)).toHours())} hours and ${((delta.abs().toMinutes()) - ((delta.abs().plusMinutes(5)).toHours() * 60)) / 5 * 5} minutes",
                        "de" to "in ${((delta.abs().plusMinutes(5)).toHours())} Stunden und ${((delta.abs().toMinutes()) - ((delta.abs().plusMinutes(5)).toHours() * 60)) / 5 * 5} Minuten",
                        "cs" to "za ${((delta.abs().plusMinutes(5)).toHours())} hodin a ${((delta.abs().toMinutes()) - ((delta.abs().plusMinutes(5)).toHours() * 60)) / 5 * 5} minut",
                        "fr" to "dans ${((delta.abs().plusMinutes(5)).toHours())} heures and ${((delta.abs().toMinutes()) - ((delta.abs().plusMinutes(5)).toHours() * 60)) / 5 * 5} minutes"
                    )[language] ?: unsupportedLanguage()
                }
                detail > 0 -> {
                    return mapOf(
                        "en" to "in ${((delta.abs().plusMinutes(30)).toHours())} hours",
                        "de" to "in ${((delta.abs().plusMinutes(30)).toHours())} Stunden",
                        "cs" to "za ${((delta.abs().plusMinutes(30)).toHours())} hodiny",
                        "fr" to "dans ${((delta.abs().plusMinutes(30)).toHours())} heures"
                    )[language] ?: unsupportedLanguage()
                }
                else -> {
                    return mapOf(
                        "en" to "in a few hours",
                        "de" to "in wenigen Stunden",
                        "cs" to "za pár hodin",
                        "fr" to "dans quelques heures"
                    )[language] ?: unsupportedLanguage()
                }
            }
        }
    } else if (data.date.toLocalDate() == today.toLocalDate()) { // events that took/take place in days around the current date
        when {
            detail == 0 && !delta.isNegative -> {
                return mapOf(
                    "en" to "earlier today",
                    "de" to "heute früher",
                    "cs" to "během dneška",
                    "fr" to "plus tôt aujourd'hui"
                )[language] ?: unsupportedLanguage()
            }
            detail == 0 -> {
                return mapOf(
                    "en" to "later today",
                    "de" to "im Laufe des Tages",
                    "cs" to "během dneška",
                    "fr" to "plus tard aujourd'hui"
                )[language] ?: unsupportedLanguage()
            }
            else -> {
                return mapOf( //TODO: days in different languages
                    "en" to ("today " + timeOfDay(data, detail)).trim().replace("today in the evening", "tonight"),
                    "de" to ("heute " + timeOfDay(data, detail)).trim(),
                    "cs" to ("dnes " + timeOfDay(data, detail)).trim(),
                    "fr" to ("aujourd'hui " + timeOfDay(data, detail)).trim()
                )[language] ?: unsupportedLanguage()
            }
        }
    } else if (delta.seconds.absoluteValue < 172800 && (data.date.toLocalDate() == yesterday.toLocalDate())) {
        return mapOf( //TODO: days in different languages
            "en" to ("yesterday " + timeOfDay(data, detail)).trim(),
            "de" to ("gestern " + timeOfDay(data, detail)).trim(),
            "cs" to ("včera " + timeOfDay(data, detail)).trim(),
            "fr" to ("heir " + timeOfDay(data, detail)).trim()
        )[language] ?: unsupportedLanguage()
    } else if (delta.seconds.absoluteValue < 172800 && (data.date.toLocalDate() == tomorrow.toLocalDate())) {
        return mapOf( //TODO: days in different languages
            "en" to ("tomorrow " + timeOfDay(data, detail)).trim(),
            "de" to ("morgen " + timeOfDay(data, detail)).trim(),
            "cs" to ("zítra " + timeOfDay(data, detail)).trim(),
            "fr" to ("demain " + timeOfDay(data, detail)).trim()
        )[language] ?: unsupportedLanguage()
    } else if (delta.seconds.absoluteValue < 604800) { //TODO Gender of days in CZ, day name localization
        return mapOf( //TODO: days in different languages
            "en" to ((if (!delta.isNegative) "last" else "next") + " ${data.dayOfWeekName} " + timeOfDay(data, if (detail > 2) 2 else detail)).trim(),
            "de" to ((if (!delta.isNegative) "letzten" else "nächsten") + " ${data.dayOfWeekName} " + timeOfDay(data, if (detail > 2) 2 else detail)).trim(),
            "cs" to ((if (!delta.isNegative) "minulou" else "příští") + " ${data.dayOfWeekName} " + timeOfDay(data, if (detail > 2) 2 else detail)).trim(),
            "fr" to ("${data.dayOfWeekName} " + (if (!delta.isNegative) "dernier" else "prochain") + " " + timeOfDay(data, if (detail > 2) 2 else detail)).trim()
        )[language] ?: unsupportedLanguage()
    } else if (delta.seconds.absoluteValue < 864000) { // events with longer time difference
        when {
            detail == 0 && !delta.isNegative -> {
                return mapOf(
                    "en" to "a week ago",
                    "de" to "vor einer Woche",
                    "cs" to "před týdnem",
                    "fr" to "il y a une semaine"
                )[language] ?: unsupportedLanguage()
            }
            detail == 0 -> {
                return mapOf(
                    "en" to "in a week",
                    "de" to "in einer Woche",
                    "cs" to "za týden",
                    "fr" to "dans une semaine"
                )[language] ?: unsupportedLanguage()
            }
            detail == 1 && !delta.isNegative -> {
                return mapOf(
                    "en" to "${delta.toDays().absoluteValue} days ago",
                    "de" to "vor ${delta.toDays().absoluteValue} tagen",
                    "cs" to "před ${delta.toDays().absoluteValue} dny",
                    "fr" to "il y a ${delta.toDays().absoluteValue} jours"
                )[language] ?: unsupportedLanguage()
            }
            detail == 1 -> {
                return mapOf(
                    "en" to "in ${delta.toDays().absoluteValue} days",
                    "de" to "in ${delta.toDays().absoluteValue} tagen",
                    "cs" to "za ${delta.toDays().absoluteValue} dní",
                    "fr" to "dans ${delta.toDays().absoluteValue} jours"
                )[language] ?: unsupportedLanguage()
            }
            else -> {
                return (dateInMonth(data) + " " + timeOfDay(data, (if (detail == 2) 0 else 2))).trim()
            }
        }
    } else if (delta.seconds.absoluteValue < 1382400) {
        when {
            detail == 0 && !delta.isNegative -> {
                return mapOf(
                    "en" to "two weeks ago",
                    "de" to "vor zwei Wochen",
                    "cs" to "před dvěma týdny",
                    "fr" to "il y a deux semaines"
                )[language] ?: unsupportedLanguage()
            }
            detail == 0 -> {
                return mapOf(
                    "en" to "in two weeks",
                    "de" to "in zwei Wochen",
                    "cs" to "za dva týdny",
                    "fr" to "en deux semaines"
                )[language] ?: unsupportedLanguage()
            }
            detail == 1 && !delta.isNegative -> {
                return mapOf(
                    "en" to "${delta.toDays().absoluteValue} days ago",
                    "de" to "vor ${delta.toDays().absoluteValue} tagen",
                    "cs" to "před ${delta.toDays().absoluteValue} dny",
                    "fr" to "il y a ${delta.toDays().absoluteValue} jours"
                )[language] ?: unsupportedLanguage()
            }
            detail == 1 -> {
                return mapOf(
                    "en" to "in ${delta.toDays().absoluteValue} days",
                    "de" to "in ${delta.toDays().absoluteValue} tagen",
                    "cs" to "za ${delta.toDays().absoluteValue} dní",
                    "fr" to "dans ${delta.toDays().absoluteValue} jours"
                )[language] ?: unsupportedLanguage()
            }
            else -> {
                return (dateInMonth(data) + " " + timeOfDay(data, (if (detail == 2) 0 else 2))).trim()
            }
        }
    } else if (delta.seconds.absoluteValue < 2678400 && data.monthValue == now.monthValue) {
        when {
            detail == 0 && !delta.isNegative -> {
                return mapOf(
                    "en" to "a few weeks ago",
                    "de" to "vor ein paar Wochen",
                    "cs" to "před pár týdny",
                    "fr" to "il y a quelques semaines"
                )[language] ?: unsupportedLanguage()
            }
            detail == 0 -> {
                return mapOf(
                    "en" to "in a few weeks",
                    "de" to "in ein paar Wochen",
                    "cs" to "za pár týdnů",
                    "fr" to "dans quelques semaines"
                )[language] ?: unsupportedLanguage()
            }
            detail == 1 && !delta.isNegative -> {
                return mapOf(
                    "en" to "${delta.toDays().absoluteValue} days ago",
                    "de" to "vor ${delta.toDays().absoluteValue} tagen",
                    "cs" to "před ${delta.toDays().absoluteValue} dny",
                    "fr" to "il y a ${delta.toDays().absoluteValue} jours"
                )[language] ?: unsupportedLanguage()
            }
            detail == 1 -> {
                return mapOf(
                    "en" to "in ${delta.toDays().absoluteValue} days",
                    "de" to "in ${delta.toDays().absoluteValue} tagen",
                    "cs" to "za ${delta.toDays().absoluteValue} dní",
                    "fr" to "dans ${delta.toDays().absoluteValue} jours"
                )[language] ?: unsupportedLanguage()
            }
            else -> {
                return (dateInMonth(data) + " " + timeOfDay(data, (if (detail == 2) 0 else 2))).trim()
            }
        }
    } else if (delta.seconds.absoluteValue < 5356800 && (data.monthValue == now.monthValue - 1 || (data.monthValue == 12 && now.monthValue == 1))) {
        return if (detail == 0) {
            mapOf(
                "en" to "last month",
                "de" to "Im vergangenen Monat",
                "cs" to "minulý měsíc",
                "fr" to "le mois dernier"
            )[language] ?: unsupportedLanguage()
        } else {
            (dateInMonth(data) + " " + timeOfDay(data, (if (detail < 3) 0 else 2))).trim()
        }
    } else if (delta.seconds.absoluteValue < 5356800 && (data.monthValue == now.monthValue + 1 || (data.monthValue == 1 && now.monthValue == 12))) {
        return if (detail == 0) {
            mapOf(
                "en" to "next month",
                "de" to "nächsten Monat",
                "cs" to "příští měsíc",
                "fr" to "le mois prochain"
            )[language] ?: unsupportedLanguage()
        } else {
            (dateInMonth(data) + " " + timeOfDay(data, (if (detail < 3) 0 else 2))).trim()
        }
    } else if (data.year == now.year) {
        return if (detail < 2) {
            mapOf(
                "en" to "in ${data.monthName}",
                "de" to "im ${data.monthName}",
                "cs" to "v ${data.monthName}",
                "fr" to "en ${data.monthName}"
            )[language] ?: unsupportedLanguage()
        } else {
            (dateInMonth(data) + " " + timeOfDay(data, detail - 2)).trim()
        }
    } else return describeDate(data, detail)
}

fun BasicDialogue.timeOfDay(data: DateTime = now, detail: Int = 0): String {
    if (detail < 1) return ""
    when {
        (data.hour == 12) -> {
            return mapOf(
                "en" to "at noon",
                "de" to "um Mittag",
                "cs" to "v poledne",
                "fr" to "à midi"
            )[language] ?: unsupportedLanguage()
        }
        (data.hour == 0) -> {
            return mapOf(
                "en" to "at midnight",
                "de" to "um Mitternacht",
                "cs" to "o půlnoci",
                "fr" to "à minuit"
            )[language] ?: unsupportedLanguage()
        }
        detail > 2 -> {
            return mapOf(
                "en" to "at " + (if (data.hour < 13) "${data.hour}" else "${data.hour - 12}") + ":${String.format("%02d", data.minute)}" + (if (data.hour < 12) " AM" else " PM"),
                "de" to "um ${data.hour}:${String.format("%02d", data.minute)} Uhr",
                "cs" to "v ${data.hour}:${String.format("%02d", data.minute)}",
                "fr" to "à ${data.hour}:${String.format("%02d", data.minute)}"
            )[language] ?: unsupportedLanguage()
        }
        detail > 1 -> {
            return mapOf(
                "en" to "around " + (if (data.hour < 13) "${data.hour}" else "${data.hour - 12}") + (if (data.hour < 12) " AM" else " PM"),
                "de" to "gegen ${data.hour} Uhr",
                "cs" to "kolem ${data.hour} hodin",
                "fr" to "vers ${data.hour} h"
            )[language] ?: unsupportedLanguage()
        }
        detail > 0 && (data.hour > 3) && (data.hour < 12) -> {
            return mapOf(
                "en" to "in the morning",
                "de" to "Morgen",
                "cs" to "ráno",
                "fr" to "matin"
            )[language] ?: unsupportedLanguage()
        }
        detail > 0 && (data.hour > 3) && (data.hour < 18) -> {
            return mapOf(
                "en" to "in the afternoon",
                "de" to "nachmittag",
                "cs" to "odpoledne",
                "fr" to "dans l'après-midi"
            )[language] ?: unsupportedLanguage()
        }
        detail > 0 && (data.hour >= 18 ) && (data.hour < 22) -> {
            return mapOf(
                "en" to "in the evening",
                "de" to "abends",
                "cs" to "večer",
                "fr" to "dans la soirée"
            )[language] ?: unsupportedLanguage()
        }
        detail > 0 -> {
            return mapOf(
                "en" to "at night",
                "de" to "nachts",
                "cs" to "v noci",
                "fr" to "la nuit"
            )[language] ?: unsupportedLanguage()
        }
    }
    return ""
}

fun BasicDialogue.dateInMonth(data: DateTime = now): String { //TODO Lokalizace mesicu
    return mapOf(
        "en" to "on ${data.monthName} ${data.dayOfMonth},",
        "de" to "am ${data.dayOfMonth} ${data.monthName}",
        "cs" to "${data.dayOfMonth} ${data.monthName}",
        "fr" to "le ${data.dayOfMonth} ${data.monthName}"
    )[language] ?: unsupportedLanguage()
}