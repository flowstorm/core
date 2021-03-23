package ai.flowstorm.common.config

interface Config {
    fun get(key: String, defaultValue: String): String = getOrNull(key) ?: defaultValue
    fun getOrNull(key: String): String?
    operator fun get(key: String): String = getOrNull(key) ?: throw NullPointerException("Missing config value $key")

    //TODO remove - should be read only
    operator fun set(key: String, value: String)
}
