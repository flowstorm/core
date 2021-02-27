package ai.flowstorm.common

import ai.flowstorm.util.LoggerDelegate
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
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
    private val logger by LoggerDelegate()

    init {
        var loaded = false
        try {
            // from resource
            val stream = javaClass.getResourceAsStream("/$FILENAME")
            if (stream != null) {
                properties.load(stream)
                loaded = true
            }

            listOf(
                    (System.getProperty("app.config") ?: ".") + "/" + FILENAME,
                    (System.getProperty("app.config") ?: ".") + "/" + LOCAL_FILENAME
            ).forEach { path ->
                File(path).let { file ->
                    if (file.exists()) {
                        logger.info("Loading config file ${file.absolutePath}")
                        properties.load(FileInputStream(file))
                        loaded = true
                    }
                }
            }
            if (get("config.log", "false") == "true")
                properties.forEach {
                    logger.info("${it.key}=${it.value}")
            }
        } catch (e: FileNotFoundException) {
            if (!loaded)
                logger.warn(e.message)
        } catch (e: Throwable) {
            logger.error("Config load error")
        }
    }

    fun get(key: String, defaultValue: String): String {
        return if (properties[key] == null) defaultValue else properties[key] as String
    }

    fun getOrNull(key: String): String? = properties[key] as String?

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
        const val LOCAL_FILENAME = "app.local.properties"

        @JvmStatic
        val instance = AppConfig()
        val version by lazy {
            instance.get("app.version", instance.get("git.ref", instance.get("git.commit", "undefined")))
        }

        @JvmStatic
        fun main(args: Array<String>) {
            println(instance)
        }
    }
}
