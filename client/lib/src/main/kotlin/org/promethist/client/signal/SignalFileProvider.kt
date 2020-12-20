package org.promethist.client.signal

import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.BasicFileAttributes


class SignalFileProvider(
        name: String,
        format: Format,
        val file: String,
        enabled: Boolean = true,
        sleep: Long = 1000,
        continuous: Boolean = false
) : SignalConfigurableProvider(name, format, enabled, sleep, continuous) {

    private var sequenceId = 0
    private var lastModified = 0L

    override fun load() = file.replace("##", sequenceId++.toString().padStart(2, '0')).let { path ->
        val path = Paths.get(path)
        val attr = Files.readAttributes(path, BasicFileAttributes::class.java)
        val lastModifiedTime = attr.lastModifiedTime().toMillis()
        if (lastModifiedTime > lastModified) {
            lastModified = lastModifiedTime
            logger.debug("Loading signal data from file $path")
            load(Files.newInputStream(path, StandardOpenOption.READ))
        }
    }

    override fun toString() = this::class.simpleName + "(name = $name, format = $format, continuous = $continuous, file = $file)"
}