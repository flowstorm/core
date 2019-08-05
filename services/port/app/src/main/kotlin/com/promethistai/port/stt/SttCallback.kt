package com.promethistai.port.stt

interface SttCallback {

    fun onResponse(transcript: String, confidence: Float, final: Boolean)

    fun onOpen()

    fun onError(e: Throwable)
}