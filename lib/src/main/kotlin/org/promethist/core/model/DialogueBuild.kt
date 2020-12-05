package org.promethist.core.model

data class DialogueBuild(
        val _id: String,
        val success: Boolean,
        val logs: List<String> = listOf(),
        val error: String = ""
)