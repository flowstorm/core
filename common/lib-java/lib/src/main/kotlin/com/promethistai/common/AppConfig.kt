package com.promethistai.common

import org.slf4j.LoggerFactory
import java.io.FileInputStream
import java.io.Serializable
import java.lang.NullPointerException
import java.util.*

/**
 * This class implements application configuation. Configuration file named AppConfig.FILENAME can be placed in
 * current working directory or directory specified by system property app.config and also in classpath
 * (root namespace). Properties defined in config placed in classpath will be overriden by values defined in
 * config taken from filesystem.
 */
class AppConfig: Serializable, Cloneable {

    private val properties: Properties = Properties()
    private var logger = LoggerFactory.getLogger(AppConfig::class.qualifiedName)

    init {
        try {
            val stream = javaClass.getResourceAsStream("/$FILENAME")
            if (stream != null)
                properties.load(stream)
            val file = (System.getProperty("app.config")?:".") + "/$FILENAME"
            properties.load(FileInputStream(file))
        } catch (e: Throwable) {
            logger.info(e.message)
        }
    }

    fun get(key: String, defaultValue: String): String {
        return if (properties[key] == null) defaultValue else properties[key] as String
    }

    operator fun get(key: String): String {
        return if (properties[key] == null)
            throw NullPointerException("Missing config property $key")
        else
            properties[key] as String
    }

    operator fun set(key: String, value: Any) {
        properties[key] = value
    }

    override fun toString(): String {
        return "${javaClass.simpleName}$properties"
    }

    companion object {
        const val FILENAME = "app.properties"

        @JvmStatic
        val instance = AppConfig()

        @JvmStatic
        fun main(args: Array<String>) {
            println(instance)
        }
    }
}
