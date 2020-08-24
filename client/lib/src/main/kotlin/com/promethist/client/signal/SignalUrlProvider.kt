package com.promethist.client.signal

import java.net.URL

class SignalUrlProvider(name: String, format: Format, enabled: Boolean = true, val url: URL, val timeout: Int) :
        SignalConfigProvider(name, format, enabled) {

    override fun run() {
        TODO("Not yet implemented")
    }
}