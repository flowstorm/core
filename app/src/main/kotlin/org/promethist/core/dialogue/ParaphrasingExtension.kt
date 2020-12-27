package org.promethist.core.dialogue

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import org.promethist.common.ObjectUtil
import org.promethist.util.TextExpander.expand
import java.util.regex.Pattern
import kotlin.random.Random

object ParaphrasingExtension {

    @JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy::class)
    private data class Rules(val minLength: Int, val maxLength: Int, var mustContain: List<String>,
                             var replaces: List<List<String>>, var headRemove: List<String>, var tailRemove: List<String>,
                             var headAddStatement: List<String>, var headAddQuestion: List<String>)

    private val rules = ObjectUtil.defaultMapper.readValue(
            ParaphrasingExtension::class.java.getResourceAsStream("paraphrasingRules.json"),
            Rules::class.java)

    private var replacePattern = mutableListOf<String>()
    private val replaceBy = mutableMapOf<String, String>()

    fun isParaphrasable(message: String): Boolean {
        val trimmedMessage = removeHead(removeTail(message.toLowerCase()))
        val numberOfTokens = trimmedMessage.split(" ").size
        if (numberOfTokens < rules.minLength || numberOfTokens > rules.maxLength) {
            return false
        }
        val pattern = "(${rules.mustContain.joinToString("|")})"
        return trimmedMessage.matches(".*\\b${pattern.toLowerCase()}\\b.*".toRegex())
    }

    fun raw(message: String) = replace(removeHead(removeTail(message.toLowerCase())))

    private fun removeHead(message: String) = message
            .replaceFirst("^(${rules.headRemove.joinToString("|")})\\b".toRegex(), "").trim()

    private fun removeTail(message: String): String {
        val pattern = "(${rules.tailRemove.joinToString("|")})"
        return message.replaceFirst("\\b" + pattern + "$".toRegex(), "").trim()
    }

    private fun replace(message: String): String {
        val pattern = "(${replacePattern.joinToString("|")})"
        val p = Pattern.compile("\\b$pattern\\b(?=(([^']|$)))")
        val m = p.matcher(message)
        var newMessage = ""
        var end = 0
        while (m.find()) {
            val start = m.start()
            val replacement = expand(replaceBy[m.group().trim()]!!).random()
            newMessage += message.substring(end, start) + " " + replacement + " "
            end = m.end()
        }
        newMessage += message.substring(end)
        return newMessage.trim().replace(" +".toRegex(), " ")
    }

    private fun isQuestion(paraphrased: String): Boolean {
        val patterns = listOf("if", "where", "when", "why", "how", "what", "who", "which", "whom", "whose")
        val pattern = "(" + java.lang.String.join("|", patterns) + ")"
        val p = Pattern.compile("^$pattern\\b")
        val m = p.matcher(paraphrased)
        return m.find()
    }

    fun paraphrase(message: String): String {
        val paraphrased = raw(message)
        val prefix = (if (isQuestion(paraphrased)) rules.headAddQuestion else rules.headAddStatement).random()
        val punctuation = if (isQuestion(paraphrased) || !isParaphrasable(message)) "?" else "."
        return "$prefix $paraphrased$punctuation".replace(" +".toRegex(), " ")
    }

    fun withProbability(message: String, higherProb: Double = 0.3, lowerProb: Double = 0.15): String {
        return if (isParaphrasable(message)) {
            if (Random.nextFloat() < higherProb) paraphrase(message) else ""
        } else {
            if (Random.nextFloat() < lowerProb && !isQuestion(message) && message.split(" ").size < 4) {
                paraphrase(message)
            } else {
                ""
            }
        }
    }

    operator fun invoke(message: String) = paraphrase(message)

    init {
        rules.mustContain = rules.mustContain.flatMap { expand(it.toLowerCase()) }
        rules.headRemove = rules.headRemove.flatMap { expand(it.toLowerCase()) }.sortedByDescending { it.length }
        rules.tailRemove = rules.tailRemove.flatMap { expand(it.toLowerCase()) }.sortedByDescending { it.length }

        rules.replaces.forEach { rule ->
            expand(rule[0].toLowerCase()).forEach {text ->
                this.replacePattern.add(text.trim())
                this.replaceBy[text.trim()] = rule[1].trim()
            }
        }
        replacePattern = replacePattern.sortedByDescending { it.length }.toMutableList()
        rules.headAddStatement = rules.headAddStatement.flatMap { expand(it.toLowerCase()) }
        rules.headAddQuestion = rules.headAddQuestion.flatMap { expand(it.toLowerCase()) }
    }
}

val BasicDialogue.paraphrasing get() = ParaphrasingExtension