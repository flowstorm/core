package com.promethist.client.signal

import com.promethist.core.type.PropertyMap
import java.net.HttpURLConnection
import java.net.URL

class SignalUrlProvider(name: String, format: Format, enabled: Boolean = true, val url: URL, sleep: Long, continuous: Boolean = false, private val method: String = "GET", private val headers: PropertyMap = mapOf(), private val timeout: Int = 5000) :
        SignalConfigurableProvider(name, format, enabled, sleep, false) {

    override fun load() = with (url.openConnection() as HttpURLConnection) {
        logger.debug("loading signal data from URL $url")
        readTimeout = timeout
        connectTimeout = timeout
        requestMethod = method
        doInput = true
        doOutput = false
        headers.entries.forEach {
            setRequestProperty(it.key, it.value.toString())
        }
        connect()
        if (responseCode > 399)
            error("$responseCode: $responseMessage")
        load(inputStream)
    }

    override fun toString() = this::class.simpleName + "(name = $name, format = $format, url = $url)"
}