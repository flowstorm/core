package com.promethist.core.dialogue

import com.promethist.core.Input
import com.promethist.core.type.DateTime
import com.promethist.core.type.Location
import java.time.DayOfWeek

// strings

fun String.toLocation() = Location(0F, 0F)

fun String.endsWith(suffixes: Collection<String>): Boolean {
    for (suffix in suffixes)
        if (endsWith(suffix))
            return true
    return false
}

infix fun String.similarityTo(tokens: List<Input.Word>): Float {
    val theseWords = tokenize().map { it.text.toLowerCase() }
    val thoseWords = tokens.map { it.text.toLowerCase() }
    var matches = 0
    for (word in theseWords)
        if (thoseWords.contains(word))
            matches++
    return matches / theseWords.size.toFloat()
}

infix fun String.similarityTo(input: Input) = similarityTo(input.words)

infix fun String.similarityTo(text: String) = similarityTo(text.tokenize())

// collections

fun <T> Collection<T>.list(transform: T.() -> String) = map { transform(it) }

fun Map<String, Any>.list(transform: Map.Entry<String, Any>.() -> String) = map { transform(it) }

fun <T> Collection<T>.random(a: Int): Collection<T> = shuffled().take(a)

// date time

val DateTime.day get() = this + 0
val DateTime.isWeekend get() = dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY

infix fun DateTime.differsInDaysFrom(dateTime: DateTime) =
        (year * 366 * 24 + hour) - (dateTime.year * 366 * 24 + dateTime.hour)

infix fun DateTime.differsInHoursFrom(dateTime: DateTime) =
        (year * 366 * 24 + hour) - (dateTime.year * 366 * 24 + dateTime.hour)

infix fun DateTime.differsInMonthsFrom(dateTime: DateTime) =
        (year * 12 + monthValue) - (dateTime.year * 12 + dateTime.monthValue)

infix fun DateTime.isSameDayAs(to: DateTime) = day == to.day

infix operator fun DateTime.plus(dayCount: Long) = day(dayCount)

infix operator fun DateTime.minus(dayCount: Long) = day(-dayCount)

infix fun DateTime.day(dayCount: Long): DateTime =
        plus(dayCount, java.time.temporal.ChronoUnit.DAYS).with(java.time.LocalTime.of(0, 0, 0, 0))

val DateTime.isDay get() = hour == 0 && minute == 0 && second == 0 && nano == 0