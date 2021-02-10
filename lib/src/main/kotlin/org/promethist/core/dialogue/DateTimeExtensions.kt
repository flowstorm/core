package org.promethist.core.dialogue

import org.promethist.core.language.English
import org.promethist.core.type.DateTime
import java.time.Duration
import java.time.ZoneId
import java.time.temporal.ChronoUnit

val DateTime.isWeekend get() = dayOfWeek == java.time.DayOfWeek.SATURDAY || dayOfWeek == java.time.DayOfWeek.SUNDAY

val DateTime.monthName get() = English.months[month.value - 1] //TODO localize
val DateTime.dayOfWeekName get() = English.weekDays[dayOfWeek.value - 1] //TODO localize

infix fun DateTime.differsInHoursFrom(other: DateTime) = Duration.between(this, other).toHours()

infix fun DateTime.differsInDaysFrom(other: DateTime) = Duration.between(this, other).toDays()

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
