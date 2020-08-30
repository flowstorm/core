package com.promethist.client.signal

class Signal(val name: String, val threshold: Double = 1.0, val timeout: Long = 0, val resetValue: Boolean = false, val requiredValue: Any? = null)