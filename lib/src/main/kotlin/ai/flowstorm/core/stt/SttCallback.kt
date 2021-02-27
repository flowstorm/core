package ai.flowstorm.core.stt

import ai.flowstorm.core.Input

interface SttCallback {

    fun onResponse(input: Input, isFinal: Boolean)

    fun onOpen()

    fun onError(e: Throwable)

    fun onEndOfUtterance()
}