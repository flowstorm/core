package com.promethistai.core.model

interface Function {
    var source: String
    enum class Type { Javascript, Kotlin }
}