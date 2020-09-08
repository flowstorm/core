package com.promethist.client.signal

import java.net.Socket

class SignalSocketProvider(name: String, format: Format, enabled: Boolean = true, continuous: Boolean = true, private val host: String, private val port: Int) :
        SignalConfigurableProvider(name, format, enabled, 0, continuous) {

    override fun load() {
        logger.debug("loading signal data from socket $host:$port")
        Socket(host, port).use {
            load(it.getInputStream())
        }
    }

    override fun toString() = this::class.simpleName + "(name = $name, format = $format, continuous = $continuous, host = $host, port = $port)"
}