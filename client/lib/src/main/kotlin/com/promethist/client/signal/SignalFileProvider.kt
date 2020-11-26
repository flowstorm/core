package com.promethist.client.signal

import java.nio.file.*

class SignalFileProvider(name: String, format: Format, val file: String, enabled: Boolean = true, sleep: Long = 1000, continuous: Boolean = false) :
        SignalConfigurableProvider(name, format, enabled, sleep, continuous) {

    private var sequenceId = 0

    override fun load() = file.replace("##", sequenceId++.toString().padStart(2, '0')).let {
        logger.debug("loading signal data from file $it")
        load(Files.newInputStream(Paths.get(it), StandardOpenOption.READ))
    }

    override fun toString() = this::class.simpleName + "(name = $name, format = $format, continuous = $continuous, file = $file)"
}