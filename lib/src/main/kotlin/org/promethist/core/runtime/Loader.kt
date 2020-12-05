package org.promethist.core.runtime

import com.fasterxml.jackson.core.type.TypeReference
import org.promethist.core.type.PropertyMap
import kotlin.reflect.KClass

interface Loader {

    fun <T : Any> loadClass(name: String): KClass<T>
    fun <T : Any> loadObject(name: String, typeReference: TypeReference<T>): T
    fun loadText(name: String): String
    fun <T : Any> newObject(name: String, vararg args: Any?): T
    fun <T : Any> newObjectWithArgs(name: String, args:PropertyMap): T
}