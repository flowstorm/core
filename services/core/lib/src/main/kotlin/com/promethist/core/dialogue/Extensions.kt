package com.promethist.core.dialogue

import com.promethist.core.Input
import com.promethist.core.type.DateTime
import com.promethist.core.type.Location
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalTime
import java.time.temporal.ChronoUnit

// strings

fun String.toLocation() = Location(0.0, 0.0)

fun String.endsWith(suffixes: Collection<String>): Boolean {
    for (suffix in suffixes)
        if (endsWith(suffix))
            return true
    return false
}

infix fun String.similarityTo(tokens: List<Input.Word>): Double {
    val theseWords = tokenize().map { it.text.toLowerCase() }
    val thoseWords = tokens.map { it.text.toLowerCase() }
    var matches = 0
    for (word in theseWords)
        if (thoseWords.contains(word))
            matches++
    return matches / theseWords.size.toDouble()
}

infix fun String.similarityTo(input: Input) = similarityTo(input.words)

infix fun String.similarityTo(text: String) = similarityTo(text.tokenize())

// collections

fun <T> Collection<T>.list(transform: T.() -> String) = map { transform(it) }

fun Map<String, Any>.list(transform: Map.Entry<String, Any>.() -> String) = map { transform(it) }

fun <T> Collection<T>.random(a: Int): Collection<T> = shuffled().take(a)

fun <T> Collection<T>.similarTo(tokens: List<Input.Word>, transform: T.() -> String, n: Int, minSimilarity: Double = .0) =
        filter { minSimilarity == .0 || transform(it) similarityTo tokens >= minSimilarity }
                .sortedByDescending { transform(it) similarityTo tokens }.take(n)

fun <T> Collection<T>.similarTo(input: Input, transform: T.() -> String, n: Int, minSimilarity: Double = .0) =
        similarTo(input.words, transform, n, minSimilarity)

fun <T> Collection<T>.similarTo(text: String, transform: T.() -> String, n: Int, minSimilarity: Double = .0) =
        similarTo(text.tokenize(), transform, n, minSimilarity)

fun <T> Collection<T>.similarTo(tokens: List<Input.Word>, transform: T.() -> String, minSimilarity: Double = .0) =
        similarTo(tokens, transform, 1, minSimilarity).firstOrNull()

fun <T> Collection<T>.similarTo(input: Input, transform: T.() -> String, minSimilarity: Double = .0) =
        similarTo(input, transform, 1, minSimilarity).firstOrNull()

fun <T> Collection<T>.similarTo(text: String, transform: T.() -> String, minSimilarity: Double = .0) =
        similarTo(text, transform, 1, minSimilarity).firstOrNull()

fun Collection<String>.similarTo(tokens: List<Input.Word>, n: Int, minSimilarity: Double = .0) =
        similarTo(tokens, { this }, n, minSimilarity)

fun Collection<String>.similarTo(input: Input, n: Int, minSimilarity: Double = .0) =
        similarTo(input, { this }, n, minSimilarity)

fun Collection<String>.similarTo(text: String, n: Int, minSimilarity: Double = .0) =
        similarTo(text, { this }, n, minSimilarity)

fun Collection<String>.similarTo(tokens: List<Input.Word>, minSimilarity: Double = .0) =
        similarTo(tokens, { this }, minSimilarity)

fun Collection<String>.similarTo(input: Input, minSimilarity: Double = .0) =
        similarTo(input, { this }, minSimilarity)

fun Collection<String>.similarTo(text: String, minSimilarity: Double = .0) =
        similarTo(text, { this }, minSimilarity)

// date time

val DateTime.isWeekend get() = dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY

infix fun DateTime.differsInHoursFrom(other: DateTime) = Duration.between(this, other).toHours()

infix fun DateTime.differsInDaysFrom(other: DateTime) = Duration.between(this, other).toDays()

infix fun DateTime.differsInMonthsFrom(other: DateTime) =
        (year * 12 + monthValue) - (other.year * 12 + other.monthValue)

infix fun DateTime.isSameDayAs(to: DateTime) =
        (dayOfYear == to.dayOfYear) && (monthValue == to.monthValue) && (year == to.year)

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

val DateTime.date get() = with(LocalTime.of(0, 0, 0, 0))
val DateTime.isDate get() = hour == 0 && minute == 0 && second == 0 && nano == 0


