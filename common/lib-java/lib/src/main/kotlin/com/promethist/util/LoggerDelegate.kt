package com.promethist.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Logger delegate allows lazy initialization of logger and setting name based on class name
 */
class LoggerDelegate : ReadOnlyProperty<Any?, Logger> {

    private lateinit var logger: Logger

    override operator fun getValue(thisRef: Any?, property: KProperty<*>): Logger {
        require(thisRef != null) { "LoggerDelegate can be used only on class properties." }

        //remove $companion from logger name
        val javaClass = thisRef.javaClass
        val clazz = if (javaClass.kotlin.isCompanion) javaClass.enclosingClass else javaClass

        if (!::logger.isInitialized) logger = createLogger(clazz)

        return logger
    }

    companion object {
        private fun <T> createLogger(clazz: Class<T>): Logger = LoggerFactory.getLogger(clazz)
    }
}