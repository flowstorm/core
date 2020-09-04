package com.promethist.client.signal

import com.fasterxml.jackson.module.kotlin.readValue
import com.promethist.client.gps.NMEA
import com.promethist.common.ObjectUtil.defaultMapper
import com.promethist.util.LoggerDelegate
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.Exception
import java.util.*

abstract class SignalConfigurableProvider(val name: String, val format: Format, val enabled: Boolean, val sleep: Long, val continuous: Boolean) : SignalProvider {

    enum class Format { Simple, Properties, JSON, NMEA }

    override lateinit var processor: SignalProcessor
    protected val logger by LoggerDelegate()

    protected fun load(input: InputStream) {
        val reader = BufferedReader(InputStreamReader(input))
        val values = mutableMapOf<String, Any>()
        do {
            if (format == Format.JSON) {
                values.putAll(defaultMapper.readValue(reader))
            } else if (format == Format.Properties && !continuous) {
                // read all properties
                while (true) {
                    (reader.readLine() ?: break).split('=').let {
                        values[it[0]] = loadValueFromString(it[1])
                    }
                }
            } else {
                val line = reader.readLine() ?: break
                when (format) {
                    Format.Properties -> line.split('=').let {
                        values[it[0]] = loadValueFromString(it[1])
                    }
                    Format.NMEA ->
                        NMEA.parse(line).let {
                            if (it.longitude != null && it.latitude != null) // load only known location
                                values[name] = it
                        }
                    Format.Simple ->
                        values[name] = loadValueFromString(line)
                }
            }
            if (continuous) {
                processor.process(values)
                values.clear()
            }
        } while (continuous)
        if (!continuous)
            processor.process(values)
    }

    abstract fun load()

    override fun run() {
        logger.info("running $this")
        while (true) {
            try {
                load()
                Thread.sleep(sleep)
            } catch (e: Exception) {
                logger.error("signal load failed", e)
                Thread.sleep(5000)
            }
        }
    }

    private fun loadValueFromString(str: String) =
            when {
                str.startsWith('"') -> str.substring(1, str.length - 2)
                Regex("true|false", RegexOption.IGNORE_CASE).matches(str) -> str.toLowerCase().toBoolean()
                Regex("\\d+").matches(str) -> str.toInt()
                Regex("[\\d]*\\.[\\d]+").matches(str) -> str.toDouble()
                else -> str
            }
}