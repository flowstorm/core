package com.promethist.core.dialogue

import com.promethist.core.Input
import com.promethist.core.type.DateTime
import com.promethist.core.type.Dynamic
import com.promethist.core.type.Location
import com.promethist.core.type.TimeValue
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.*

abstract class BasicDialogue : Dialogue() {

    enum class Article { None, Indefinite, Definite }

    companion object {

        val pass: Transition? = null
        @Deprecated("Use pass instead, toIntent will be removed")
        val toIntent = pass
        val now: DateTime get() = with (threadContext()) { DateTime.now(context.turn.input.zoneId) }
        val today get() = now.toDay()
        val tomorrow get() = now.day(1)
        val yesterday get() = now.day(-1)

        infix fun DateTime.isSameDateAs(to: DateTime) = true

        fun DateTime.day(dayCount: Long): DateTime =
                plus(dayCount, ChronoUnit.DAYS).with(LocalTime.of(0, 0, 0, 0))

        fun DateTime.toDay() = day(0)

        fun DateTime.isDay(from: Long, to: Long = from): Boolean {
            val thisDay = day(0)
            return now.day(from) <= thisDay && thisDay < now.day(to + 1)
        }

        fun DateTime.isToday() = isDay(0, 0)
        fun DateTime.isTomorrow() = isDay(1, 1)
        fun DateTime.isYesterday() = isDay(-1, -1)
        fun DateTime.isWeekend() = now.let { it.dayOfWeek == DayOfWeek.SATURDAY || it.dayOfWeek == DayOfWeek.SUNDAY }
        fun DateTime.isHoliday() = isWeekend()
        infix fun DateTime.differsInDaysFrom(dateTime: DateTime) =
                (year * 366 * 24 + hour) - (dateTime.year * 366 * 24 + dateTime.hour)
        infix fun DateTime.differsInHoursFrom(dateTime: DateTime) =
                (year * 366 * 24 + hour) - (dateTime.year * 366 * 24 + dateTime.hour)
        infix fun DateTime.differsInMonthsFrom(dateTime: DateTime) =
                (year * 12 + monthValue) - (dateTime.year * 12 + dateTime.monthValue)
    }

    val turnAttributes get() = with (threadContext()) { context.turn.attributes(dialogueNameWithoutVersion) }

    val sessionAttributes get() = with (threadContext()) { context.session.attributes(dialogueNameWithoutVersion) }

    val userAttributes get() = with (threadContext()) { context.userProfile.attributes(dialogueNameWithoutVersion) }

    fun communityAttributes(communityName: String) = with (threadContext()) { context.communityResource.get(communityName)?.attributes ?: Dynamic.EMPTY }


    private inline fun unsupportedLanguage(): Nothing {
        val stackTraceElement = Thread.currentThread().stackTrace[1]
        throw error("${stackTraceElement.className}.${stackTraceElement.methodName} does not support language ${language} of dialogue ${dialogueName}")
    }

    fun enumerate(col: Collection<String>, subject: String = "", article: Article = Article.None): String {
        val list = if (col is List<String>) col else col.toList()
        when {
            list.isEmpty() ->
                return empty(subject)
            list.size == 1 ->
                return article(list.first(), article) + (if (subject.isNotEmpty()) " $subject" else "")
            else -> {
                val and = mapOf("en" to "and", "de" to "und", "cs" to "a")[language] ?: unsupportedLanguage()
                val str = StringBuilder()
                for (i in list.indices) {
                    if (i > 0)
                        str.append(if (i == list.size - 1) ", $and " else ", ")
                    str.append(article(list[i], article))
                }
                if (subject.isNotEmpty())
                    str.append(' ').append(plural(subject))
                return str.toString()
            }
        }
    }

    fun enumerateWithArticle(col: Collection<String>, subject: String = "") = enumerate(col, subject, Article.Indefinite)

    fun enumerateWithDefiniteArticle(col: Collection<String>, subject: String = "") = enumerate(col, subject, Article.Definite)

    fun describe(map: Map<String, Any>): String {
        val list = mutableListOf<String>()
        val isWord = when (language) {
            "en" -> "is"
            "de" -> "ist"
            "cs" -> "je"
            else -> unsupportedLanguage()
        }
        map.forEach {
            list.add("${it.key} $isWord " + describe(it.value))
        }
        return enumerate(list)
    }

    fun describe(col: Collection<String>) = enumerate(col)

    fun describe(tm: TimeValue<*>) = describe(tm.value) + indent(describe(tm.time, 2))

    fun describe(value: Any?, detailLevel: Int = 0) =
        when (value) {
            is Location -> "latitude ${value.latitude}, longitude ${value.longitude}"
            is DateTime -> value.toString()
            is String -> value
            null -> "unknown"
            else -> value.toString()
        }

    fun describeMore(value: Any?) = describe(value, 1)

    fun describeDetailed(value: Any?) = describe(value, 2)

    fun enumerate(map: Map<String, Number>): String = enumerate(mutableListOf<String>().apply {
        map.forEach {
            add(it.value of it.key)
        }
    })

    fun empty(subject: String) =
        when (language) {
            "en" -> "no"
            "de" -> "kein" //TODO male vs. female
            else -> unsupportedLanguage()
        } + " $subject"

    fun lemma(word: String) = word

    fun plural(subject: String, cond: (() -> Boolean)? = null) =
        if (cond == null || cond())
            when (language) {
                "en" -> if (subject.endsWith("s")) subject else subject + "s"
                else -> unsupportedLanguage()
            }
        else this

    fun article(subject: String, article: Article = Article.None) =
        when (language) {
            "en" -> when (article) {
                Article.Indefinite -> (if (subject.startsWithVowel()) "an " else "a ") + subject
                Article.Definite -> "the $subject"
                else -> subject
            }
            else -> subject
        }

    fun indent(value: Any?) = (value?.let { " " + describe(value) } ?: "")

    fun greeting(name: String? = null) =
        (
                if (now.hour >= 18 || now.hour < 3)
                    mapOf(
                            "en" to "good evening",
                            "de" to "guten abend",
                            "cs" to "dobrý večer",
                            "fr" to "bonsoir"
                    )[language] ?: unsupportedLanguage()
                else if (now.hour < 12)
                    mapOf(
                            "en" to "good morning",
                            "de" to "guten morgen",
                            "cs" to "dobré ráno",
                            "fr" to "bonjour"
                    )[language] ?: unsupportedLanguage()
                else
                    mapOf(
                            "en" to "good afternoon",
                            "de" to "guten tag",
                            "cs" to "dobré odpoledne",
                            "fr" to "bonne après-midi"
                    )[language] ?: unsupportedLanguage()
                ) + indent(name)

    fun definiteArticle(subject: String) = article(subject, Article.Definite)

    infix fun Number.of(subject: String) =
            when (this) {
                0 -> empty(subject)
                1 -> "$this $subject"
                else -> "$this " + plural(subject)
            }

    infix fun Array<*>.of(subject: String) = size of subject

    infix fun Collection<*>.of(subject: String) = size of subject

    infix fun Map<*, *>.of(subject: String) = size of subject
}

fun String.startsWithVowel() = Regex("[aioy].*").matches(this)

fun String.tokenize(): List<Input.Word> {
    val tokens = mutableListOf<Input.Word>()
    val tokenizer = StringTokenizer(this, " \t\n\r,.:;?![]'")
    while (tokenizer.hasMoreTokens()) {
        tokens.add(Input.Word(tokenizer.nextToken().toLowerCase()))
    }
    return tokens
}