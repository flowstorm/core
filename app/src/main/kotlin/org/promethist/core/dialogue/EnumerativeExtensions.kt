package org.promethist.core.dialogue

import org.promethist.core.type.Dynamic

fun BasicDialogue.enumerate(vararg data: Any?, subjBlock: (Int) -> String, before: Boolean = false, conj: String = "", detail: Int = 0) =
        enumerate(data.asList().map { describe(it, detail) }, subjBlock, before, conj)

fun BasicDialogue.enumerate(vararg data: Any?, subj: String = "", before: Boolean = false, conj: String = "", detail: Int = 0) =
        enumerate(data.asList().map { describe(it, detail) }, subj, before, conj)

fun BasicDialogue.enumerate(data: Collection<String>, subjBlock: (Int) -> String, before: Boolean = false, conj: String = ""): String {
    val list = if (data is List<String>) data else data.toList()
    val subj = subjBlock(list.size)
    when {
        list.isEmpty() ->
            return empty(subj)
        list.size == 1 ->
            return (if (before && subj.isNotEmpty()) "$subj " else "") +
                    list.first() +
                    (if (!before && subj.isNotEmpty()) " $subj" else "")
        else -> {
            val op = if (conj == "")
                mapOf("en" to "and", "de" to "und", "cs" to "a")[language] ?: unsupportedLanguage()
            else
                conj
            val str = StringBuilder()
            if (before && subj.isNotEmpty())
                str.append(subj).append(' ')
            for (i in list.indices) {
                if (i > 0)
                    str.append(if (i == list.size - 1) ", $op " else ", ")
                str.append(list[i])
            }
            if (!before && subj.isNotEmpty())
                str.append(' ').append(subj)
            return str.toString()
        }
    }
}

fun BasicDialogue.enumerate(subjBlock: (Int) -> String, data: Collection<String>, conj: String = "") =
        enumerate(data, subjBlock, true, conj)

fun BasicDialogue.enumerate(subj: String, data: Collection<String>, conj: String = "") =
        enumerate(data, subj, true, conj)

fun BasicDialogue.enumerate(data: Collection<String>, subj: String = "", before: Boolean = false, conj: String = "") =
        enumerate(data, { plural(subj, data.size) }, before, conj)

fun BasicDialogue.enumerate(data: Collection<Number>, before: Boolean = false, conj: String = "") =
        enumerate(data.map { describe(it) }, "", before, conj)

fun BasicDialogue.enumerate(data: Map<String, Number>): String = enumerate(mutableListOf<String>().apply {
    data.forEach { add(it.value of it.key) }
})

fun BasicDialogue.enumerate(data: Dynamic) = enumerate(mutableListOf<String>().apply {
    data.forEach {
        if (it.value is Number)
            add(it.value as Number of it.key)
    }
})

fun BasicDialogue.enumerate(vararg pairs: Pair<String, Number>) = enumerate(pairs.toMap())