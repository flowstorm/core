package com.promethist.client.signal

class Signal(val name: String, val type: Type, val threshold: Int = 1, val resetValue: Boolean = false) {
    enum class Type { Flag, Number, Text }
}