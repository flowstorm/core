package com.promethist.core.runtime

import com.promethist.common.ObjectUtil
import java.io.InputStream
import java.io.InputStreamReader
import javax.script.ScriptEngineManager
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

abstract class AbstractLoader : Loader {

    data class CacheItem(val item: Any, val lastModified: Long)

    private val engine = ScriptEngineManager().getEngineByExtension("kts")
            ?: error("Kotlin script engine not available (check program resources)")
    private val cache: MutableMap<String, CacheItem> = mutableMapOf()

    private fun <T: Any> cache(name: String, load: () -> T): T {
        val lastModified = getLastModified(name)
        if (!cache.containsKey(name) || (cache[name]!!.lastModified < lastModified)) {
            cache[name] = CacheItem(load(), lastModified)
        }
        return cache[name]!!.item as T
    }

    abstract fun getInputStream(name: String): InputStream

    abstract fun getLastModified(name: String): Long

    override fun loadClass(name: String): KClass<*> = cache(name) {
        //TODO check if $name.class exists > load via Java class loader
        (engine.eval(InputStreamReader(getInputStream("$name.kts")))
            ?: error("Cannot load class from resource $name (probably missing export expression)")) as KClass<*>
    }

    override fun loadObject(name: String): Map<String, Any> = cache(name) {
        ObjectUtil.defaultMapper.readValue<Map<*, *>>(getInputStream("$name.json"), Map::class.java) as Map<String, Any>
    }

    override fun loadText(name: String): String = cache(name) {
        String(getInputStream("$name.txt").readBytes())
    }

    override fun <T : Any> loadObject(name: String, type: KClass<T>): T = cache(name) {
        ObjectUtil.defaultMapper.readValue<T>(getInputStream("$name.json"), type.java) as T
    }

    override fun <T : Any> newObject(name: String, vararg args: Any?): T {
        val clazz = loadClass(name) as KClass<T>
        return clazz.primaryConstructor?.call(this, name, *args)?:error("Missing primary constructor for $clazz")
    }
}