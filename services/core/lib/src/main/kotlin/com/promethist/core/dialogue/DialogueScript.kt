package com.promethist.core.dialogue

import com.promethist.core.Input
import com.promethist.core.type.Dynamic
import com.promethist.core.type.Location
import com.promethist.core.type.TimeValue
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.*

open class DialogueScript {

    enum class Article { None, Indefinite, Definite }

    val clientNamespace = "client"
    val pass: Dialogue.Transition? = null
    val toIntent get() = with (Dialogue.threadContext()) { Dialogue.Transition(dialogue.intentNode(context)) }
    val now get() = with (Dialogue.threadContext()) { ZonedDateTime.now(context.turn.input.zoneId) }
    val today get() = now.toDay()
    val tomorrow get() = now.day(1)
    val yesterday get() = now.day(-1)

    val turnAttributes get() = with (Dialogue.threadContext()) { context.turn.attributes(dialogue.nameWithoutVersion) }

    val sessionAttributes get() = with (Dialogue.threadContext()) { context.session.attributes(dialogue.nameWithoutVersion) }

    val userAttributes get() = with (Dialogue.threadContext()) { context.userProfile.attributes(dialogue.nameWithoutVersion) }

    fun communityAttributes(communityName: String) = with (Dialogue.threadContext()) { context.communityResource.get(communityName)?.attributes ?: Dynamic.EMPTY }

    private inline fun unsupportedLanguage(): Nothing = with (Dialogue.threadContext()) {
        val stackTraceElement = Thread.currentThread().stackTrace[1]
        throw error("${stackTraceElement.className}.${stackTraceElement.methodName} does not support language ${dialogue.language} of dialogue ${dialogue.name}")
    }

    fun enumerate(col: Collection<String>, subject: String = "", article: Article = Article.None) = with (Dialogue.threadContext()) {
        val list = if (col is List<String>) col else col.toList()
        when {
            list.isEmpty() -> empty(subject)
            list.size == 1 ->
                article(list.first(), article) + (if (subject.isNotEmpty()) " $subject" else "")
            else -> {
                val and = mapOf("en" to "and", "de" to "und", "cs" to "a")[dialogue.language] ?: unsupportedLanguage()
                val str = StringBuilder()
                for (i in list.indices) {
                    if (i > 0)
                        str.append(if (i == list.size - 1) ", $and " else ", ")
                    str.append(article(list[i], article))
                }
                if (subject.isNotEmpty())
                    str.append(' ').append(plural(subject))
                str.toString()
            }
        }
    }

    fun enumerateWithArticle(col: Collection<String>, subject: String = "") = enumerate(col, subject, Article.Indefinite)

    fun enumerateWithDefiniteArticle(col: Collection<String>, subject: String = "") = enumerate(col, subject, Article.Definite)

    fun describe(map: Map<String, Any>) = with (Dialogue.threadContext()) {
        val list = mutableListOf<String>()
        val isWord = when (dialogue.language) {
            "en" -> "is"
            "de" -> "ist"
            "cs" -> "je"
            else -> unsupportedLanguage()
        }
        map.forEach { item ->
            list.add("${item.key} $isWord " + describe(item.value))
        }
        enumerate(list)
    }

    fun describe(col: Collection<String>) = enumerate(col)

    fun describe(tm: TimeValue<*>) = describe(tm.value) + indent(describe(tm.time, 2))

    fun describe(value: Any?, detailLevel: Int = 0) = with (Dialogue.threadContext()) {
        when (value) {
            is Location -> "latitude ${value.latitude}, longitude ${value.longitude}"
            is ZonedDateTime -> value.toString()
            is String -> value
            null -> "unknown"
            else -> value.toString()
        }
    }

    fun describeMore(value: Any?) = describe(1)

    fun describeDetailed(value: Any?) = describe(2)

    fun enumerate(map: Map<String, Number>): String = enumerate(mutableListOf<String>().apply {
        map.forEach {
            add(it.value of it.key)
        }
    })

    fun empty(subject: String) = with (Dialogue.threadContext()) {
        when (dialogue.language) {
            "en" -> "no"
            "de" -> "kein" //TODO male vs. female
            else -> unsupportedLanguage()
        } + " $subject"
    }

    fun lemma(word: String) = word

    fun plural(subject: String, cond: (() -> Boolean)? = null) = with (Dialogue.threadContext()) {
        if (cond == null || cond())
            when (dialogue.language) {
                "en" -> if (subject.endsWith("s")) subject else subject + "s"
                else -> unsupportedLanguage()
            }
        else this
    }

    fun article(subject: String, article: Article = Article.None) = with (Dialogue.threadContext()) {
        when (dialogue.language) {
            "en" -> when (article) {
                Article.Indefinite -> (if (subject.startsWithVowel()) "an " else "a ") + subject
                Article.Definite -> "the $subject"
                else -> subject
            }
            else -> subject
        }
    }

    fun indent(value: Any?) = (value?.let { " " + describe(value) } ?: "")

    fun greeting(name: String? = null) = with (Dialogue.threadContext()) {
        (
            if (now.hour >= 18 || now.hour < 3)
                mapOf(
                        "en" to "good evening",
                        "de" to "guten abend",
                        "cs" to "dobrý večer",
                        "fr" to "bonsoir"
                )[dialogue.language] ?: unsupportedLanguage()
            else if (now.hour < 12)
                mapOf(
                        "en" to "good morning",
                        "de" to "guten morgen",
                        "cs" to "dobré ráno",
                        "fr" to "bonjour"
                )[dialogue.language] ?: unsupportedLanguage()
            else
                mapOf(
                        "en" to "good afternoon",
                        "de" to "guten tag",
                        "cs" to "dobré odpoledne",
                        "fr" to "bonne après-midi"
                )[dialogue.language] ?: unsupportedLanguage()
        ) + indent(name)
    }

    fun definiteArticle(subject: String) = article(subject, Article.Definite)

    fun String.toLocation() = Location(0F, 0F)

    infix fun String.similarityTo(tokens: List<Input.Word>): Float {
        val words = toLowerCase().split(" ", ",", ".", ":", ";")
        var matches = 0
        for (i in 0 until if (words.size < tokens.size) words.size else tokens.size)
            if (words[i].trim() == tokens[i].text)
                matches++
        return matches / words.size.toFloat()
    }

    infix fun String.similarityTo(input: Input) = similarityTo(input.words)

    infix fun String.similarityTo(text: String) = similarityTo(text.tokenize())

    infix fun Number.of(subject: String) =
            when (this) {
                0 -> empty(subject)
                1 -> "$this $subject"
                else -> "$this " + plural(subject)
            }

    infix fun Array<*>.of(subject: String) = size of subject

    infix fun Collection<*>.of(subject: String) = size of subject

    infix fun Map<*, *>.of(subject: String) = size of subject

    infix fun ZonedDateTime.isSameDateAs(to: ZonedDateTime) = true

    fun ZonedDateTime.day(dayCount: Long): ZonedDateTime =
            plus(dayCount, ChronoUnit.DAYS).with(LocalTime.of(0, 0, 0, 0))

    fun ZonedDateTime.toDay() = day(0)

    fun ZonedDateTime.isDay(from: Long, to: Long = from): Boolean {
        val thisDay = day(0)
        return now.day(from) <= thisDay && thisDay < now.day(to + 1)
    }

    fun ZonedDateTime.isToday() = isDay(0, 0)
    fun ZonedDateTime.isTomorrow() = isDay(1, 1)
    fun ZonedDateTime.isYesterday() = isDay(-1, -1)
    fun ZonedDateTime.isWeekend() = now.let { it.dayOfWeek == DayOfWeek.SATURDAY || it.dayOfWeek == DayOfWeek.SUNDAY }
    fun ZonedDateTime.isHoliday() = isWeekend()
    infix fun ZonedDateTime.differsInDaysFrom(dateTime: ZonedDateTime) =
            (year * 366 * 24 + hour) - (dateTime.year * 366 * 24 + dateTime.hour)
    infix fun ZonedDateTime.differsInHoursFrom(dateTime: ZonedDateTime) =
            (year * 366 * 24 + hour) - (dateTime.year * 366 * 24 + dateTime.hour)
    infix fun ZonedDateTime.differsInMonthsFrom(dateTime: ZonedDateTime) =
            (year * 12 + monthValue) - (dateTime.year * 12 + dateTime.monthValue)
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