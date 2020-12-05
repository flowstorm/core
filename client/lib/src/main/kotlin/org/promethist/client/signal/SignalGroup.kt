package org.promethist.client.signal

class SignalGroup(val name: String, val type: Type = Type.Text, val signals: Array<Signal>) {
    enum class Type { Text, Touch }
}