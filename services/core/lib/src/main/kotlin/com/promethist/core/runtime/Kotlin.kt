package com.promethist.core.runtime

import java.io.Reader
import javax.script.ScriptEngineManager
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

object Kotlin {

    val engine = ScriptEngineManager().getEngineByExtension("kts")
            ?: error("Kotlin script engine not available (check program resources)")

    fun <T : Any> loadClass(reader: Reader): KClass<T> {
        val clazz = engine.eval(reader)
        if (clazz == null)
            error("Cannot load class (probably missing export expression)")
        else
            return clazz as KClass<T>
    }

    fun <T : Any> newObject(clazz: KClass<T>, vararg args: Any?): T =
            clazz.primaryConstructor?.call(*args)?:error("Missing primary constructor for $clazz")
}