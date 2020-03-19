package com.promethist.core.runtime

import com.promethist.common.ObjectUtil
import java.io.InputStream
import java.io.InputStreamReader
import javax.script.ScriptEngineManager
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

abstract class AbstractLoader : Loader {

    data class CacheItem(val item: Any, val lastModified: Long)

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

    override fun <T : Any> loadClass(name: String): KClass<T> = cache(name) {
        //TODO check if $name.class exists > load via Java class loader
        Kotlin.loadClass(InputStreamReader(getInputStream("$name.kts")))
    }

    override fun <T : Any> loadObject(name: String): T = cache(name) {
        ObjectUtil.defaultMapper.readValue(getInputStream("$name.json"), Map::class.java) as T
    }

    override fun loadText(name: String): String = cache(name) {
        String(getInputStream("$name.txt").readBytes())
    }

    override fun <T : Any> loadObject(name: String, type: KClass<T>): T = cache(name) {
        ObjectUtil.defaultMapper.readValue<T>(getInputStream("$name.json"), type.java) as T
    }

    override fun <T : Any> newObject(name: String, vararg args: Any?): T = Kotlin.newObject(loadClass(name), this, name, *args)
}