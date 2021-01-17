package org.promethist.core.builder

import org.promethist.core.model.DialogueSourceCode

data class Request(
    val sourceCode: DialogueSourceCode,
    val oodExamples: List<String>
)