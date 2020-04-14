package com.promethist.core.dialogue

import java.time.LocalDate
import java.time.LocalDateTime

open class DialogueScript {

    enum class Article { None, Indefinite, Definite }

    val now get() = LocalDateTime.now()
    val today get() = LocalDate.now()

    inline fun unsupportedLanguage(): Nothing = Dialogue.threadContext().let {
        val stackTraceElement = Thread.currentThread().stackTrace[1]
        throw error("${stackTraceElement.className}.${stackTraceElement.methodName} does not support language ${it.dialogue.language} of dialogue ${it.dialogue.name}")
    }

    fun emptyWord(subject: String? = null) = Dialogue.threadContext().let {
        when (it.dialogue.language) {
            "en" -> "no"
            "de" -> "kein" //TODO male vs. female
            else -> unsupportedLanguage()
        } + (subject?.let {
            //TODO declension
            " $subject"
        } ?: "")
    }

    fun withArticle(word: String, article: Article = Article.None) = Dialogue.threadContext().let {
        when (it.dialogue.language) {
            "en" -> when (article) {
                Article.Indefinite -> (if (word.startsWithVowel()) "an " else "a ") + word
                Article.Definite -> "the $word"
                else -> word
            }
            else -> word
        }
    }

    fun quantify(subject: String, count: Number) =
            when (count) {
                0 -> emptyWord(subject)
                1 -> "$count $subject"
                else -> "$count " + subject.pluralize()
            }
    fun enumerate(list: List<String>, subject: String? = null, article: Article = Article.None): String = Dialogue.threadContext().let {
        when {
            list.isEmpty() -> Dialogue.emptyWord(subject)
            list.size == 1 ->
                withArticle(list.first(), article) + subject?.let { " $subject" }
            else -> {
                val and = mapOf("en" to "and", "de" to "und", "cs" to "a")[it.dialogue.language] ?: Dialogue.unsupportedLanguage()
                val str = StringBuilder()
                for (i in list.indices) {
                    if (i > 0)
                        str.append(if (i == list.size - 1) " $and " else ", ")
                    str.append(withArticle(list[i], article))
                }
                if (subject != null)
                    str.append(' ').append(subject.pluralize())
                str.toString()
            }
        }
    }
}

fun String.startsWithVowel() = Regex("[aioy].*").matches(this)

fun String.withArticle() = Dialogue.withArticle(this, DialogueScript.Article.Indefinite)

fun String.pluralize(cond: (() -> Boolean)? = null) = Dialogue.threadContext().let {
    if (cond == null || cond())
        when (it.dialogue.language) {
            "en" -> this + "s"
            else -> Dialogue.unsupportedLanguage()
        }
    else this
}

fun Collection<*>.quantify(subject: String): String = Dialogue.quantify(subject, size)

fun Collection<String>.enumerate(subject: String? = null): String = Dialogue.enumerate(toList(), subject)

fun Collection<String>.enumerateWithArticle(subject: String? = null): String =
        Dialogue.enumerate(toList(), subject, DialogueScript.Article.Indefinite)

fun Collection<String>.enumerateWithDefiniteArticle(subject: String? = null): String =
        Dialogue.enumerate(toList(), subject, DialogueScript.Article.Definite)

fun Map<String, Any>.enumerate() = Dialogue.threadContext().let {
    val list = mutableListOf<String>()
    forEach {
        list.add(Dialogue.quantify(it.key, if (it.value is Number) it.value as Number else 0))
    }
    list.enumerate()
}

fun Map<String, Any>.speak() = Dialogue.threadContext().let {
    val list = mutableListOf<String>()
    val isWord = when (it.dialogue.language) {
        "en" -> "is"
        "de" -> "ist"
        "cs" -> "je"
        else -> Dialogue.unsupportedLanguage()
    }
    forEach {
        list.add("${it.key} $isWord ${it.value}")
    }
    list.enumerate()
}

fun LocalDateTime.speak() = "TODO LocalDateTime.speech"
fun LocalDate.speak() = "TODO LocalDate.speech"