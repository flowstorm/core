package com.promethist.core.model

data class Build(
        val _id: String,
        val success:Boolean,
        val logs: List<String> = listOf(),
        val error: String = ""
)