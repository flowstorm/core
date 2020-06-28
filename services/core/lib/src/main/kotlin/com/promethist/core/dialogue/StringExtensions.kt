package com.promethist.core.dialogue

import com.promethist.core.Input
import com.promethist.core.type.Location
import java.util.*

fun String.startsWithVowel() = Regex("[aioy].*").matches(this)

fun String.tokenize(): List<Input.Word> {
    val tokens = mutableListOf<Input.Word>()
    val tokenizer = StringTokenizer(this, " \t\n\r,.:;?![]'")
    while (tokenizer.hasMoreTokens()) {
        tokens.add(Input.Word(tokenizer.nextToken().toLowerCase()))
    }
    return tokens
}

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



