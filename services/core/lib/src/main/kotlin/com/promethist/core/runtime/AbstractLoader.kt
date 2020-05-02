package com.promethist.core.runtime

import com.fasterxml.jackson.core.type.TypeReference
import com.promethist.core.type.PropertyMap
import com.promethist.common.ObjectUtil.defaultMapper as mapper
import com.promethist.util.LoggerDelegate
import java.io.InputStream
import java.io.InputStreamReader
import java.util.*
import kotlin.jvm.internal.Reflection
import kotlin.reflect.KClass

abstract class AbstractLoader(open val noCache: Boolean, open val useScript: Boolean) : Loader {

    data class CacheItem(val item: Any, val lastModified: Long)

    protected val logger by LoggerDelegate()
    private val cache: MutableMap<String, CacheItem> = mutableMapOf()
    private val byteCodeClassLoader = DialogueClassLoader(this::class.java.classLoader)

    private fun <T: Any> cache(name: String, load: () -> T): T {
        val lastModified = getLastModified(name)
        if (noCache || !cache.containsKey(name) || (cache[name]!!.lastModified < lastModified)) {
            cache[name] = CacheItem(load(), lastModified)
        }
        @Suppress("UNCHECKED_CAST")
        return cache[name]!!.item as T
    }

    abstract fun getInputStream(name: String): InputStream

    abstract fun getLastModified(name: String): Long

    override fun <T : Any> loadClass(name: String): KClass<T> = if (useScript) {
        cache("$name.kts") {
            logger.info("loading class $name from $name.kts file")
            Kotlin.loadClass<T>(InputStreamReader(getInputStream("$name.kts")))
        }
    } else {
        cache("$name.properties") {
            logger.info("loading class $name from $name/*.class file(s)")
            val properties = Properties()
            properties.load(getInputStream("$name.properties"))
            val buildId = properties.getProperty("buildId")
            properties.getProperty("classes").split(",").forEach { simpleName ->
                val className = "model.$buildId.$simpleName"
                val classPath = "$name/$simpleName.class"
                logger.info("loading java class $className from $classPath")
                byteCodeClassLoader.loadClass(className, getInputStream(classPath).readBytes())
            }
            val javaClass = byteCodeClassLoader.loadClass("model.$buildId.Model")
            Reflection.createKotlinClass(javaClass) as KClass<T>
        }
    }

    override fun loadText(name: String): String = cache("$name.txt") {
        logger.info("loading text $name")
        String(getInputStream("$name.txt").readBytes())
    }

    override fun <T : Any> loadObject(name: String, typeReference: TypeReference<T>): T = cache("$name.json") {
        logger.info("loading object $name")
        mapper.readValue<T>(getInputStream("$name.json"), typeReference)
    }

    override fun <T : Any> newObject(name: String, vararg args: Any?): T {
        logger.info("creating object $name ${args.toList()}")
        return Kotlin.newObject(loadClass(name), *args)
    }

    override fun <T : Any> newObjectWithArgs(name: String, args: PropertyMap): T {
        logger.info("creating object $name ${args.toList()}")
        return Kotlin.newObjectWithArgs(loadClass(name), args)
    }
}