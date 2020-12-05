package org.promethist.client.signal

import java.io.BufferedReader
import java.io.InputStreamReader

class SignalProcessProvider(name: String, format: Format, enabled: Boolean = true, continuous: Boolean = true, private val command: String) :
        SignalConfigurableProvider(name, format, enabled, 0, continuous) {

    override fun load() {
        logger.debug("loading signal data from process $command")
        ProcessBuilder(*command.split(' ').toTypedArray()).apply {
            val proc = start()
            Thread {
                BufferedReader(InputStreamReader(proc.errorStream)).use {
                    while (true) println(it.readLine() ?: break)
                }
            }.start()
            load(proc.inputStream)
            val exit = proc.waitFor()
            if (exit != 0) {
                logger.error("${this@SignalProcessProvider} error $exit")
                Thread.sleep(5000)
            }
        }
    }

    override fun toString() = this::class.simpleName + "(name = $name, format = $format, continuous = $continuous, command = '$command')"
}