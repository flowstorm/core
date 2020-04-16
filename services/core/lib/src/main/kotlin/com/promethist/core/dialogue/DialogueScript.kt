package com.promethist.core.dialogue

import com.promethist.core.Input
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.reflect.KProperty

open class DialogueScript {

    enum class Article { None, Indefinite, Definite }

    val now get() = LocalDateTime.now()
    val today get() = LocalDate.now()

    private inline fun unsupportedLanguage(): Nothing = Dialogue.threadContext().let {
        val stackTraceElement = Thread.currentThread().stackTrace[1]
        throw error("${stackTraceElement.className}.${stackTraceElement.methodName} does not support language ${it.dialogue.language} of dialogue ${it.dialogue.name}")
    }

    private fun enumerate(list: List<String>, subject: String = "", article: Article = Article.None) = Dialogue.threadContext().let {
        when {
            list.isEmpty() -> subject.empty()
            list.size == 1 ->
                list.first().withArticle(article) + (if (subject.isNotEmpty()) " $subject" else "")
            else -> {
                val and = mapOf("en" to "and", "de" to "und", "cs" to "a")[it.dialogue.language] ?: unsupportedLanguage()
                val str = StringBuilder()
                for (i in list.indices) {
                    if (i > 0)
                        str.append(if (i == list.size - 1) " $and " else ", ")
                    str.append(list[i].withArticle(article))
                }
                if (subject.isNotEmpty())
                    str.append(' ').append(subject.pluralize())
                str.toString()
            }
        }
    }

    fun String.empty() = Dialogue.threadContext().let {
        when (it.dialogue.language) {
            "en" -> "no"
            "de" -> "kein" //TODO male vs. female
            else -> unsupportedLanguage()
        } + " $this"
    }

    fun String.startsWithVowel() = Regex("[aioy].*").matches(this)

    fun String.withArticle(article: Article = Article.None) = Dialogue.threadContext().let {
        when (it.dialogue.language) {
            "en" -> when (article) {
                Article.Indefinite -> (if (startsWithVowel()) "an " else "a ") + this
                Article.Definite -> "the $this"
                else -> this
            }
            else -> this
        }
    }

    fun String.lemma() = this //TODO

    fun String.pluralize(cond: (() -> Boolean)? = null) = Dialogue.threadContext().let {
        if (cond == null || cond())
            when (it.dialogue.language) {
                "en" -> this + "s"
                else -> unsupportedLanguage()
            }
        else this
    }

    fun Number.quantify(subject: String) =
            when (this) {
                0 -> subject.empty()
                1 -> "$this $subject"
                else -> "$this " + subject.pluralize()
            }

    fun Array<*>.quantify(subject: String) = size.quantify(subject)

    fun Collection<*>.quantify(subject: String) = size.quantify(subject)

    fun Map<*, *>.quantify(subject: String) = size.quantify(subject)

    fun Collection<String>.enumerate(subject: String = "") = enumerate(toList(), subject)

    fun Collection<String>.enumerateWithArticle(subject: String = "") = enumerate(toList(), subject, Article.Indefinite)

    fun Collection<String>.enumerateWithDefiniteArticle(subject: String = "") = enumerate(toList(), subject, Article.Definite)

    fun Map<String, Any>.enumerate(): String {
        val list = mutableListOf<String>()
        forEach {
            list.add((if (it.value is Number) it.value as Number else 0).quantify(it.key))
        }
        return list.enumerate()
    }

    fun Map<String, Any>.describe() = Dialogue.threadContext().let {
        val list = mutableListOf<String>()
        val isWord = when (it.dialogue.language) {
            "en" -> "is"
            "de" -> "ist"
            "cs" -> "je"
            else -> unsupportedLanguage()
        }
        forEach {
            list.add("${it.key} $isWord ${it.value}")
        }
        list.enumerate()
    }

    fun LocalDateTime.format() {
        "TODO LocalDateTime.format"
    }

    fun LocalDate.format() {
        "TODO LocalDate.format"
    }

    infix fun String.similarityTo(input: Input): Float {
        val inputWords = input.words
        val words = toLowerCase().split(" ", ",", ".", ":", ";")
        var matches = 0
        for (i in 0 until if (words.size < inputWords.size) words.size else inputWords.size)
            if (words[i].trim() == inputWords[i].text)
                matches++
        return matches / words.size.toFloat()
    }
}
