package com.promethist.client.signal

import java.io.File
import java.io.FileInputStream

class SignalFileProvider(name: String, format: Format, val file: File, enabled: Boolean = true, sleep: Long = 1000, continuous: Boolean = false) :
        SignalConfigurableProvider(name, format, enabled, sleep, continuous) {

    override fun load() = load(FileInputStream(file))

    override fun toString() = this::class.simpleName + "(name = $name, format = $format, continuous = $continuous, file = $file)"
}