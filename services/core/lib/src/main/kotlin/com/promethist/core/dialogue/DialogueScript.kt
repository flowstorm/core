package com.promethist.core.dialogue

import com.promethist.core.Input
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

open class DialogueScript {

    enum class Article { None, Indefinite, Definite }

    val now get() = LocalDateTime.now()
    val today get() = LocalDate.now()
    val clientNamespace = "client"

    private inline fun unsupportedLanguage(): Nothing = Dialogue.threadContext().let {
        val stackTraceElement = Thread.currentThread().stackTrace[1]
        throw error("${stackTraceElement.className}.${stackTraceElement.methodName} does not support language ${it.dialogue.language} of dialogue ${it.dialogue.name}")
    }

    fun enumerate(col: Collection<String>, subject: String = "", article: Article = Article.None) = Dialogue.threadContext().let {
        val list = if (col is List<String>) col else col.toList()
        when {
            list.isEmpty() -> empty(subject)
            list.size == 1 ->
                article(list.first(), article) + (if (subject.isNotEmpty()) " $subject" else "")
            else -> {
                val and = mapOf("en" to "and", "de" to "und", "cs" to "a")[it.dialogue.language] ?: unsupportedLanguage()
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

    fun describe(map: Map<String, Any>) = Dialogue.threadContext().let {
        val list = mutableListOf<String>()
        val isWord = when (it.dialogue.language) {
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

    fun describe(value: Any?, detailLevel: Int = 0) = Dialogue.threadContext().let {
        when (value) {
            is LocalDateTime -> value.toString()
            is LocalDate -> value.toString()
            is String -> value
            null -> "unknown"
            else -> value.toString()
        }
    }

    fun describeMore(value: Any?) = describe(1)

    fun describeDetailed(value: Any?) = describe(2)

    fun enumerate(map: Map<String, Number>): String {
        val list = mutableListOf<String>()
        map.forEach {
            list.add(it.value of it.key)
        }
        return enumerate(list)
    }

    fun empty(subject: String) = Dialogue.threadContext().let {
        when (it.dialogue.language) {
            "en" -> "no"
            "de" -> "kein" //TODO male vs. female
            else -> unsupportedLanguage()
        } + " $subject"
    }

    fun lemma(word: String) = word

    fun plural(subject: String, cond: (() -> Boolean)? = null) = Dialogue.threadContext().let {
        if (cond == null || cond())
            when (it.dialogue.language) {
                "en" -> if (subject.endsWith("s")) subject else subject + "s"
                else -> unsupportedLanguage()
            }
        else this
    }

    fun article(subject: String, article: Article = Article.None) = Dialogue.threadContext().let {
        when (it.dialogue.language) {
            "en" -> when (article) {
                Article.Indefinite -> (if (subject.startsWithVowel()) "an " else "a ") + subject
                Article.Definite -> "the $subject"
                else -> subject
            }
            else -> subject
        }
    }

    fun definiteArticle(subject: String) = article(subject, Article.Definite)

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