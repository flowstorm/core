package com.promethist.client.util

import java.io.OutputStream

interface AudioRecorder {

    var outputStream: OutputStream?
    fun start(sessionId: String)
    fun stop()
}