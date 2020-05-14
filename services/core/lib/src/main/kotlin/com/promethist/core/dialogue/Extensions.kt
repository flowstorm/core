package com.promethist.core.dialogue

import com.promethist.core.Input
import com.promethist.core.type.Location

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

fun <T> Collection<T>.list(transform: T.() -> String) = map { transform(it) }

fun Collection<*>.random(a: Int): Collection<*> = shuffled().take(a)