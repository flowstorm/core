package com.promethist.core.runtime

import kotlin.reflect.KClass

interface Loader {

    fun <T : Any> loadClass(name: String): KClass<T>
    fun <T : Any> loadObject(name: String): T
    fun loadText(name: String): String
    fun <T : Any> loadObject(name: String, type: KClass<T>): T
    fun <T : Any> newObject(name: String, vararg args: Any?): T
}