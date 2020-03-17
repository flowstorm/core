package com.promethist.core.runtime

import kotlin.reflect.KClass

interface Loader {

    fun loadClass(name: String): KClass<*>
    fun loadObject(name: String): Map<String, Any>
    fun loadText(name: String): String
    fun <T : Any> loadObject(name: String, type: KClass<T>): T
    fun <T : Any> newObject(name: String, vararg args: Any?): T
}