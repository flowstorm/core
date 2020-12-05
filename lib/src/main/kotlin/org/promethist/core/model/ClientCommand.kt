package org.promethist.core.model

enum class ClientCommand { version, volume_up, volume_down }

inline fun <reified T : Enum<T>> enumContains(name: String): Boolean {
    return enumValues<T>().any { it.name == name}
}