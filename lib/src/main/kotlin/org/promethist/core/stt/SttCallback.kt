package org.promethist.core.stt

import org.promethist.core.Input

interface SttCallback {

    fun onResponse(input: Input, isFinal: Boolean)

    fun onOpen()

    fun onError(e: Throwable)

    fun onEndOfUtterance()
}