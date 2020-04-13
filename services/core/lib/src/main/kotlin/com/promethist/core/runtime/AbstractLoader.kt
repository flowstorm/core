package com.promethist.core.runtime

import com.fasterxml.jackson.core.type.TypeReference
import com.promethist.common.ObjectUtil.defaultMapper as mapper
import com.promethist.util.LoggerDelegate
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.io.InputStreamReader
import kotlin.reflect.KClass

abstract class AbstractLoader(open val noCache: Boolean) : Loader {

    data class CacheItem(val item: Any, val lastModified: Long)

    protected val logger by LoggerDelegate()
    private val cache: MutableMap<String, CacheItem> = mutableMapOf()

    private fun <T: Any> cache(name: String, load: () -> T): T {
        val lastModified = getLastModified(name)
        if (noCache || !cache.containsKey(name) || (cache[name]!!.lastModified < lastModified)) {
            cache[name] = CacheItem(load(), lastModified)
        }
        return cache[name]!!.item as T
    }

    abstract fun getInputStream(name: String): InputStream

    abstract fun getLastModified(name: String): Long

    override fun <T : Any> loadClass(name: String): KClass<T> = cache("$name.kts") {
        //TODO check if $name.class exists > load via Java class loader
        logger.info("loading class $name")
        Kotlin.loadClass(InputStreamReader(getInputStream("$name.kts")))
    }

    override fun loadText(name: String): String = cache("$name.txt") {
        logger.info("loading text $name")
        String(getInputStream("$name.txt").readBytes())
    }

    override fun <T : Any> loadObject(name: String): T = cache("$name.json") {
        logger.info("loading object $name")
        mapper.readValue<T>(getInputStream("$name.json"), object : TypeReference<T>() {}) as T
    }

    override fun <T : Any> newObject(name: String, vararg args: Any?): T {
        logger.info("creating object $name ${args.toList()}")
        return Kotlin.newObject(loadClass(name), *args)
    }
}