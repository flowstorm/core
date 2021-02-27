package ai.flowstorm.client.signal

class SignalValue(var value: Any, var time: Long = System.currentTimeMillis(), var lastEmittedValue: Any? = null, var lastEmittedTime: Long? = null)