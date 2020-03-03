package com.promethist.core.model

interface Function {
    var source: String
    enum class Type { Javascript, Kotlin }
}