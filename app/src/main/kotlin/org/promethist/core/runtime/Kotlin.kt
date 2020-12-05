package org.promethist.core.runtime

import org.promethist.core.type.PropertyMap
import java.io.Reader
import java.lang.Exception
import java.lang.RuntimeException
import javax.script.ScriptEngineManager
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

object Kotlin {

    val engine by lazy {
        ScriptEngineManager().getEngineByExtension("kts")
                ?: error("Kotlin script engine not available (check program resources)")
    }

    fun <T : Any> loadClass(reader: Reader): KClass<T> {
        val clazz = engine.eval(reader)
        if (clazz == null)
            error("Cannot load class (probably missing export expression)")
        else
            return clazz as KClass<T>
    }

    fun <T : Any> newObject(clazz: KClass<T>, vararg args: Any?): T =
            clazz.primaryConstructor?.call(*args)?:error("Missing primary constructor for $clazz")

    fun <T : Any> newObjectWithArgs(clazz: KClass<T>, args: PropertyMap): T {
        try {
            requireNotNull(clazz.primaryConstructor) { "Missing primary constructor for $clazz" }
            val params = clazz.primaryConstructor!!.parameters
                    .filter { args.contains(it.name) }
                    .map { it to args[it.name] }.toMap()

            return clazz.primaryConstructor!!.callBy(params)
        } catch (e: Throwable) {
            throw RuntimeException("Can not create instance of ${clazz.qualifiedName}", e)
        }
    }
}