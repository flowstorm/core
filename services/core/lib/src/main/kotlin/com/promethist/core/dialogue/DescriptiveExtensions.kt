package com.promethist.core.dialogue

import com.promethist.core.type.DateTime
import com.promethist.core.type.Location
import com.promethist.core.type.Memory

fun BasicDialogue.describe(data: Map<String, Any>): String {
    val list = mutableListOf<String>()
    val isWord = when (language) {
        "en" -> "is"
        "de" -> "ist"
        "cs" -> "je"
        else -> unsupportedLanguage()
    }
    data.forEach {
        list.add("${it.key} $isWord " + describe(it.value))
    }
    return enumerate(list)
}

fun BasicDialogue.describe(data: Collection<String>) = enumerate(data)

fun BasicDialogue.describe(data: Memory<*>) = describe(data.value) + indent(describe(data.time, BasicDialogue.HIGH))

fun BasicDialogue.describe(data: Any?, detail: Int = 0) =
        when (data) {
            is Location -> "latitude is ${data.latitude}, longitude is ${data.longitude}"
            is DateTime -> if (data.isDate) describeDate(data, detail) else describeTime(data, detail)
            is String -> data
            null -> "undefined"
            else -> data.toString()
        }