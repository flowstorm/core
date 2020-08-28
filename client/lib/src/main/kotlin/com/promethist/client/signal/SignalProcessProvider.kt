package com.promethist.client.signal

class SignalProcessProvider(name: String, format: Format, enabled: Boolean = true, continuous: Boolean = true, private val command: String) :
        SignalConfigurableProvider(name, format, enabled, 0, continuous) {

    override fun load() {
        ProcessBuilder(*command.split(' ').toTypedArray()).apply {
            val proc = start()
            load(proc.inputStream)
            val exit = proc.waitFor()
            if (exit != 0)
                logger.error("${this@SignalProcessProvider} error $exit")
        }
    }

    override fun toString() = this::class.simpleName + "(name = $name, format = $format, continuous = $continuous, command = '$command')"
}