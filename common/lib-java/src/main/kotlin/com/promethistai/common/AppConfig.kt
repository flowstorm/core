package com.promethistai.common

import java.io.FileInputStream
import java.io.IOException
import java.io.Serializable
import java.util.*

/**
 * This class implements application configuation. Configuration file named AppConfig.FILENAME can be placed in
 * current working directory or directory specified by system property app.config and also in classpath
 * (root namespace). Properties defined in config placed in classpath will be overriden by values defined in
 * config taken from filesystem.
 */
class AppConfig: Serializable, Cloneable {

    private val properties: Properties = Properties()

    init {
        try {
            val stream = javaClass.getResourceAsStream("/$FILENAME")
            if (stream != null)
                properties.load(stream)
            val file = (System.getProperty("app.config")?:".") + "/$FILENAME"
            properties.load(FileInputStream(file))
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    operator fun get(key: String): String? {
        return if (this.properties[key] == null) null else this.properties[key] as String
    }

    operator fun set(key: String, value: Any) {
        this.properties[key] = value
    }

    override fun toString(): String {
        return "${javaClass.simpleName}$properties"
    }

    companion object {
        const val FILENAME = "app.properties"
        val instance = AppConfig()

        @JvmStatic
        fun main(args: Array<String>) {
            println(instance)
        }
    }
}
