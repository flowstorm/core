package ai.flowstorm.common

import ai.flowstorm.common.config.Config
import ai.flowstorm.common.config.PropertiesFileConfig
import java.io.Serializable

/**
 * Allow static access
 */
object AppConfig : Serializable, Cloneable, Config by PropertiesFileConfig() {
    @JvmStatic
    @Deprecated("Preferred way is avoid static access and use injection. When really need static access use AppConfig itself (it's singleton)")
    val instance: Config = this
    val version by lazy {
        get("app.version", get("git.ref", get("git.commit", "unknown")))
    }

    @JvmStatic
    fun main(args: Array<String>) {
        println(this)
    }
}
