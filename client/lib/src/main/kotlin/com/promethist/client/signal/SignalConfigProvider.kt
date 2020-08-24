package com.promethist.client.signal

abstract class SignalConfigProvider(val name: String, val format: Format, val enabled: Boolean) : SignalProvider() {
    enum class Format { Value, Properties, JSON }
}