package com.promethist.client.signal

import java.io.File

class SignalFileProvider(name: String, format: Format, val file: File, enabled: Boolean = true, val timeout: Int = 1000) :
        SignalConfigProvider(name, format, enabled) {

    override fun run() {
        TODO("Not yet implemented")
    }
}