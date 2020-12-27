package org.promethist.util

object TextExpander {
    data class Expansion(val texts: List<String>, val start: Int, val end: Int)

    fun expand(text: String): List<String> {
        if (!text.contains("(") && !text.contains(")")) {
            return listOf(text)
        }
        var start = 0
        var groupStart = 0
        var stack = 0
        val expansions = mutableListOf<Expansion>()
        var currentGroup = mutableListOf<String>()
        text.forEachIndexed { i, c ->
            if (c == '(') {
                if (stack++ == 0) {
                    start = i + 1
                    groupStart = i + 1
                    currentGroup = mutableListOf()
                }
            } else if (c == '|' && stack == 1) {
                currentGroup.addAll(expand(text.substring(start, i)))
                start = i + 1
            } else if (c == ')') {
                if (--stack == 0) {
                    currentGroup.addAll(expand(text.substring(start, i)))
                    expansions.add(Expansion(currentGroup, groupStart - 1, i + 1))
                }
            }
            if (stack < 0) {
                throw IllegalArgumentException("Missing opening parenthesis or too many closing ones.")
            }
        }
        if (stack > 0) {
            throw IllegalArgumentException("Missing closing parenthesis or too many opening ones.")
        }
        return replace(text, expansions)
    }

    private fun replace(text: String, expansions: List<Expansion>): List<String> {
        fun replace(texts: List<String>, expansions: List<Expansion>, i: Int): List<String> {
            if (i < 0) {
                return texts
            }
            val result = mutableListOf<String>()
            for (txt in texts) {
                for (replacement in expansions[i].texts) {
                    val res = (txt.substring(0, expansions[i].start) + replacement + txt.substring(expansions[i].end))
                        .trim().replace(" +".toRegex(), " ")
                    if (res.isNotBlank()) {
                        result.add(res)
                    }
                }
            }
            return replace(result, expansions, i - 1)
        }
        return replace(listOf(text), expansions, expansions.size - 1)
    }
}