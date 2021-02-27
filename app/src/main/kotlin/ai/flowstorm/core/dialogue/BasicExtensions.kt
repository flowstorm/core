package ai.flowstorm.core.dialogue

import ai.flowstorm.core.language.English
import kotlin.math.absoluteValue

enum class Article { None, Indefinite, Definite }

fun BasicDialogue.article(subj: String, article: Article = Article.Indefinite) =
        when (language) {
            "en" -> when (article) {
                Article.Indefinite -> (if (subj.startsWithVowel()) "an " else "a ") + subj
                Article.Definite -> "the $subj"
                else -> subj
            }
            else -> subj
        }

fun BasicDialogue.definiteArticle(subj: String) = article(subj, Article.Definite)

fun BasicDialogue.empty(subj: String) =
        when (language) {
            "en" -> "zero"
            "de" -> "kein" //TODO male vs. female
            else -> unsupportedLanguage()
        } + " $subj"

fun BasicDialogue.lemma(word: String) = word

fun BasicDialogue.plural(input: String, count: Int = 2) =
        if (input.isBlank()) {
            input
        } else with (if (input.indexOf('+') > 0) input else "$input+") {
            split(" ").joinToString(" ") {
                val word = if (it[it.length - 1] == '+' || it[it.length - 1] == '?')
                    it.substring(0, it.length - 1)
                else
                    it
                if (count < 1 && it.endsWith('?')) {
                    ""
                } else if (count.absoluteValue != 1 && it.endsWith('+')) {
                    when (language) {
                        "en" -> English.irregularPlurals.getOrElse(word) {
                            when {
                                word.endsWith("y") && !word.endsWith(listOf("ay", "ey", "iy", "oy", "uy", "yy")) ->
                                    word.substring(0, word.length - 1) + "ies"
                                word.endsWith(listOf("s", "sh", "ch", "x", "z", "o")) ->
                                    word + "es"
                                else ->
                                    word + "s"
                            }
                        }
                        else -> unsupportedLanguage()
                    }
                } else {
                    word
                }
            }
        }

fun BasicDialogue.plural(data: Collection<String>, count: Int = 2) = data.map { plural(it, count) }
