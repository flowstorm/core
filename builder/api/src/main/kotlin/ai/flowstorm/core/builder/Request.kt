package ai.flowstorm.core.builder

import ai.flowstorm.core.model.DialogueSourceCode

data class Request(
    val sourceCode: DialogueSourceCode,
    val oodExamples: List<String>
)