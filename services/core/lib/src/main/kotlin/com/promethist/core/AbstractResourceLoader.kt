package com.promethist.core

import com.promethist.common.ObjectUtil
import java.io.InputStream
import java.io.InputStreamReader
import javax.script.ScriptEngineManager
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

abstract class AbstractResourceLoader : ResourceLoader {

    private val engine = ScriptEngineManager().getEngineByExtension("kts")
            ?: error("Kotlin script engine not available (check program resources)")

    abstract fun getFileStream(name: String): InputStream

    override fun loadClass(name: String) =
            (engine.eval(InputStreamReader(getFileStream("$name.kts")))
                    ?: error("Cannot load class from resource $name (probably missing export)")) as KClass<*>

    override fun loadObject(name: String): Map<String, Any> =
            ObjectUtil.defaultMapper.readValue<Map<*, *>>(getFileStream("$name.json"), Map::class.java) as Map<String, Any>

    override fun loadText(name: String): String = String(getFileStream("$name.txt").readBytes())

    override fun <T : Any> loadObject(name: String, type: KClass<T>): T =
            ObjectUtil.defaultMapper.readValue<T>(getFileStream("$name.json"), type.java) as T

    override fun <T : Any> newObject(name: String, vararg args: Any?): T {
        val clazz = loadClass(name) as KClass<T>
        val constructor = clazz.primaryConstructor!!
        //println(constructor)
        return constructor.call(this, name, *args)
    }
}