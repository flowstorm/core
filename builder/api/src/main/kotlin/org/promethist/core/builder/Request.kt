package org.promethist.core.builder

import org.promethist.core.model.DialogueSourceCode
import org.promethist.core.model.DialogueSourceCode.GlobalIntent

data class Request(
    val sourceCode: DialogueSourceCode,
    val oodExamples: List<String>
)