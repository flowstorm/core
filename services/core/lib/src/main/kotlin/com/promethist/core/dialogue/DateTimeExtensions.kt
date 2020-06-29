package com.promethist.core.dialogue

import com.promethist.core.language.English
import com.promethist.core.type.DateTime
import java.time.Duration
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.absoluteValue

val DateTime.isWeekend get() = dayOfWeek == java.time.DayOfWeek.SATURDAY || dayOfWeek == java.time.DayOfWeek.SUNDAY

val DateTime.monthName get() = English.months[month.value - 1] //TODO localize
val DateTime.dayOfWeekName get() = English.weekDays[dayOfWeek.value - 1] //TODO localize

infix fun DateTime.differsInHoursFrom(other: DateTime) = java.time.Duration.between(this, other).toHours()

infix fun DateTime.differsInDaysFrom(other: DateTime) = java.time.Duration.between(this, other).toDays()

infix fun DateTime.differsInMonthsFrom(other: DateTime) =
        (year * 12 + monthValue) - (other.year * 12 + other.monthValue)

infix fun DateTime.isSameDayAs(to: DateTime) =
        (dayOfYear == to.dayOfYear) && (monthValue == to.monthValue) && (year == to.year)

fun DateTime.set(year: Int = Int.MIN_VALUE, month: Int = 0, dayOfMonth: Int = 0,
                    hour: Int = -1, minute: Int = -1, second: Int = -1, nanoOfSecond: Int = 0, zone: ZoneId? = null) =
    DateTime.of(
            if (year == Int.MIN_VALUE) this.year else year,
            if (month == 0) this.monthValue else month,
            if (dayOfMonth == 0) this.dayOfMonth else dayOfMonth,
            if (hour == -1) this.hour else hour,
            if (minute == -1) this.minute else minute,
            if (second == -1) this.second else second,
            0,
            if (zone == null) this.zone else zone
    )

class DateTimeUnit(val unit: ChronoUnit, val amount: Long)

infix operator fun DateTime.plus(timeUnit: DateTimeUnit) = plus(timeUnit.amount, timeUnit.unit)
infix operator fun DateTime.minus(timeUnit: DateTimeUnit) = minus(timeUnit.amount, timeUnit.unit)

val Number.second: DateTimeUnit get() = DateTimeUnit(ChronoUnit.SECONDS, toLong())
val Number.minute: DateTimeUnit get() = DateTimeUnit(ChronoUnit.MINUTES, toLong())
val Number.hour: DateTimeUnit get() = DateTimeUnit(ChronoUnit.HOURS, toLong())
val Number.day: DateTimeUnit get() = DateTimeUnit(ChronoUnit.DAYS, toLong())
val Number.week: DateTimeUnit get() = DateTimeUnit(ChronoUnit.WEEKS, toLong())
val Number.month: DateTimeUnit get() = DateTimeUnit(ChronoUnit.MONTHS, toLong())
val Number.year: DateTimeUnit get() = DateTimeUnit(ChronoUnit.YEARS, toLong())

val DateTime.date get() = with(java.time.LocalTime.of(0, 0, 0, 0))
val DateTime.isDate get() = hour == 0 && minute == 0 && second == 0 && nano == 0

fun BasicDialogue.describeDate(data: DateTime = now, detail: Int = 0): String {
    val delta = Duration.between(data, now)
    if (data == today) { // events that took/take place in days around the current date
        return mapOf(
                "en" to "today",
                "de" to "heute",
                "cs" to "dnes",
                "fr" to "aujourd'hui"
        )[language] ?: unsupportedLanguage()
    } else if (data == yesterday) {
        return mapOf( //TODO: days in different languages
                "en" to "yesterday",
                "de" to "gestern",
                "cs" to "včera",
                "fr" to "heir"
        )[language] ?: unsupportedLanguage()
    } else if (data == tomorrow) {
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
                "en" to "last year" + (if (detail == 1) " in ${data.month}" else if (detail > 1) ", ${dateInMonth(data)}" else ""),
                "de" to "letztes Jahr" + (if (detail == 1) " im ${data.month}" else if (detail > 1) ", ${dateInMonth(data)}" else ""),
                "cs" to "minulý rok" + (if (detail == 1) " v ${data.month}" else if (detail > 1) ", ${dateInMonth(data)}" else ""),
                "fr" to "l'année dernière" + (if (detail == 1) " en ${data.month}" else if (detail > 1) ", ${dateInMonth(data)}" else "")
        )[language] ?: unsupportedLanguage()
    } else if (data.year == now.year + 1) {
        return mapOf(
                "en" to "next year" + (if (detail == 1) " in ${data.month}" else if (detail > 1) ", ${dateInMonth(data)}" else ""),
                "de" to "nächstes Jahr" + (if (detail == 1) " im ${data.month}" else if (detail > 1) ", ${dateInMonth(data)}" else ""),
                "cs" to "příští rok" + (if (detail == 1) " v ${data.month}" else if (detail > 1) ", ${dateInMonth(data)}" else ""),
                "fr" to "l'année prochaine" + (if (detail == 1) " en ${data.month}" else if (detail > 1) ", ${dateInMonth(data)}" else "")
        )[language] ?: unsupportedLanguage()
    } else {
        return mapOf(
                "en" to (if (detail == 0) "in" else if (detail == 1) "in ${data.month}" else if (detail > 1) dateInMonth(data) else "") + " ${data.year}",
                "de" to (if (detail == 0) "in" else if (detail == 1) "im ${data.month}" else if (detail > 1) dateInMonth(data) else "") + " ${data.year}",
                "cs" to (if (detail == 0) "v roce" else if (detail == 1) "v ${data.month}" else if (detail > 1) dateInMonth(data) else "") + " ${data.year}",
                "fr" to (if (detail == 0) "en" else if (detail == 1) "en ${data.month}" else if (detail > 1) dateInMonth(data) else "") + " ${data.year}"
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
                            "en" to "${((delta.plusMinutes(5)).toHours().absoluteValue)} hours and ${((delta.toMinutes().absoluteValue) - ((delta.plusMinutes(5)).toHours().absoluteValue * 60)) / 5 * 5} minutes ago",
                            "de" to "vor ${((delta.plusMinutes(5)).toHours().absoluteValue)} Stunden und ${((delta.toMinutes().absoluteValue) - ((delta.plusMinutes(5)).toHours().absoluteValue * 60)) / 5 * 5} Minuten",
                            "cs" to "před ${((delta.plusMinutes(5)).toHours().absoluteValue)} hodinami a ${((delta.toMinutes().absoluteValue) - ((delta.plusMinutes(5)).toHours().absoluteValue * 60)) / 5 * 5} minutami",
                            "fr" to "il y a ${((delta.plusMinutes(5)).toHours().absoluteValue)} heures and ${((delta.toMinutes().absoluteValue) - ((delta.plusMinutes(5)).toHours().absoluteValue * 60)) / 5 * 5} minutes"
                    )[language] ?: unsupportedLanguage()
                }
                detail > 0 -> {
                    return mapOf(
                            "en" to "${((delta.plusMinutes(30)).toHours().absoluteValue)} hours ago",
                            "de" to "vor ${((delta.plusMinutes(30)).toHours().absoluteValue)} Stunden",
                            "cs" to "před ${((delta.plusMinutes(30)).toHours().absoluteValue)} hodinami",
                            "fr" to "il y a ${((delta.plusMinutes(30)).toHours().absoluteValue)} heures"
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
                            "en" to "in ${((delta.plusMinutes(5)).toHours().absoluteValue)} hours",
                            "de" to "in ${((delta.plusMinutes(5)).toHours().absoluteValue)} Stunden",
                            "cs" to "za ${((delta.plusMinutes(5)).toHours().absoluteValue)} hodiny",
                            "fr" to "dans ${((delta.plusMinutes(5)).toHours().absoluteValue)} heures"
                    )[language] ?: unsupportedLanguage()
                }
                detail > 2 && delta.toMinutes().absoluteValue % 60 > 5 -> {
                    return mapOf(
                            "en" to "in ${((delta.plusMinutes(5)).toHours().absoluteValue)} hours and ${((delta.toMinutes().absoluteValue) - ((delta.plusMinutes(5)).toHours().absoluteValue * 60)) / 5 * 5} minutes",
                            "de" to "in ${((delta.plusMinutes(5)).toHours().absoluteValue)} Stunden und ${((delta.toMinutes().absoluteValue) - ((delta.plusMinutes(5)).toHours().absoluteValue * 60)) / 5 * 5} Minuten",
                            "cs" to "za ${((delta.plusMinutes(5)).toHours().absoluteValue)} hodin a ${((delta.toMinutes().absoluteValue) - ((delta.plusMinutes(5)).toHours().absoluteValue * 60)) / 5 * 5} minut",
                            "fr" to "dans ${((delta.plusMinutes(5)).toHours().absoluteValue)} heures and ${((delta.toMinutes().absoluteValue) - ((delta.plusMinutes(5)).toHours().absoluteValue * 60)) / 5 * 5} minutes"
                    )[language] ?: unsupportedLanguage()
                }
                detail > 0 -> {
                    return mapOf(
                            "en" to "in ${((delta.plusMinutes(30)).toHours().absoluteValue)} hours",
                            "de" to "in ${((delta.plusMinutes(30)).toHours().absoluteValue)} Stunden",
                            "cs" to "za ${((delta.plusMinutes(30)).toHours().absoluteValue)} hodiny",
                            "fr" to "dans ${((delta.plusMinutes(30)).toHours().absoluteValue)} heures"
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
    } else if (data.date == today) { // events that took/take place in days around the current date
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
                        "en" to "today " + timeOfDay(data, detail),
                        "de" to "heute " + timeOfDay(data, detail),
                        "cs" to "dnes " + timeOfDay(data, detail),
                        "fr" to "aujourd'hui " + timeOfDay(data, detail)
                )[language] ?: unsupportedLanguage()
            }
        }
    } else if (delta.seconds.absoluteValue < 172800 && (data.date == yesterday)) {
        return mapOf( //TODO: days in different languages
                "en" to "yesterday " + timeOfDay(data, detail),
                "de" to "gestern " + timeOfDay(data, detail),
                "cs" to "včera " + timeOfDay(data, detail),
                "fr" to "heir " + timeOfDay(data, detail)
        )[language] ?: unsupportedLanguage()
    } else if (delta.seconds.absoluteValue < 172800 && (data.date == tomorrow)) {
        return mapOf( //TODO: days in different languages
                "en" to "tomorrow " + timeOfDay(data, detail),
                "de" to "morgen " + timeOfDay(data, detail),
                "cs" to "zítra " + timeOfDay(data, detail),
                "fr" to "demain " + timeOfDay(data, detail)
        )[language] ?: unsupportedLanguage()
    } else if (delta.seconds.absoluteValue < 604800) { //TODO Gender of days in CZ, day name localization
        return mapOf( //TODO: days in different languages
                "en" to (if (!delta.isNegative) "last" else "next") + " ${data.dayOfWeekName} " + timeOfDay(data, if (detail > 2) 2 else detail),
                "de" to (if (!delta.isNegative) "letzten" else "nächsten") + " ${data.dayOfWeekName} " + timeOfDay(data, if (detail > 2) 2 else detail),
                "cs" to (if (!delta.isNegative) "minulou" else "příští") + " ${data.dayOfWeekName} " + timeOfDay(data, if (detail > 2) 2 else detail),
                "fr" to "${data.dayOfWeekName} " + (if (!delta.isNegative) "dernier" else "prochain") + " " + timeOfDay(data, if (detail > 2) 2 else detail)
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
                return dateInMonth(data) + " " + timeOfDay(data, (if (detail == 2) 0 else 2))
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
                return dateInMonth(data) + " " + timeOfDay(data, (if (detail == 2) 0 else 2))
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
                return dateInMonth(data) + " " + timeOfDay(data, (if (detail == 2) 0 else 2))
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
            dateInMonth(data) + " " + timeOfDay(data, (if (detail < 3) 0 else 2))
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
            dateInMonth(data) + " " + timeOfDay(data, (if (detail < 3) 0 else 2))
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
            dateInMonth(data) + " " + timeOfDay(data, detail - 2)
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
                    "en" to "at " + (if (data.hour < 13) "${data.hour}" else "${data.hour - 12}") + ":${data.minute} " + (if (data.hour < 12) " AM" else " PM"),
                    "de" to "um ${data.hour}:${data.minute} Uhr",
                    "cs" to "v ${data.hour}:${data.minute}",
                    "fr" to "à ${data.hour}:${data.minute}"
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
        detail > 0 && (data.hour < 18) -> {
            return mapOf(
                    "en" to "in the afternoon",
                    "de" to "nachmittag",
                    "cs" to "odpoledne",
                    "fr" to "dans l'après-midi"
            )[language] ?: unsupportedLanguage()
        }
        detail > 0 -> {
            return mapOf(
                    "en" to "in the evening",
                    "de" to "abends",
                    "cs" to "večer",
                    "fr" to "dans la soirée"
            )[language] ?: unsupportedLanguage()
        }
    }
    return ""
}

fun BasicDialogue.dateInMonth(data: DateTime = now): String { //TODO Lokalizace mesicu
    return mapOf(
            "en" to "on ${data.month} ${data.dayOfMonth},",
            "de" to "am ${data.dayOfMonth} ${data.month}",
            "cs" to "${data.dayOfMonth} ${data.month}",
            "fr" to "le ${data.dayOfMonth} ${data.month}"
    )[language] ?: unsupportedLanguage()
}