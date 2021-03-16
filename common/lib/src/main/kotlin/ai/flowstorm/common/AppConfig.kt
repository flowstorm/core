package ai.flowstorm.common

import ai.flowstorm.common.config.Config
import ai.flowstorm.common.config.PropertiesFileConfig
import java.io.Serializable

/**
 * Allow static access
 */
object AppConfig : Serializable, Cloneable, Config by PropertiesFileConfig() {
    @JvmStatic
    val instance: Config = this
    val version by lazy {
        instance.get("app.version", instance.get("git.ref", instance.get("git.commit", "unknown")))
    }

    @JvmStatic
    fun main(args: Array<String>) {
        println(instance)
    }
}
