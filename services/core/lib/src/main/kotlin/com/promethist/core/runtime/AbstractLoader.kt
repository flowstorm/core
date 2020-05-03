package com.promethist.core.runtime

import com.fasterxml.jackson.core.type.TypeReference
import com.promethist.core.model.FileObject
import com.promethist.core.type.PropertyMap
import com.promethist.common.ObjectUtil.defaultMapper as mapper
import com.promethist.util.LoggerDelegate
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URLClassLoader
import kotlin.jvm.internal.Reflection
import kotlin.reflect.KClass

abstract class AbstractLoader(open val noCache: Boolean, open val useScript: Boolean) : Loader {

    data class CacheItem(val item: Any, val fileObject: FileObject)

    protected val logger by LoggerDelegate()
    private val cache: MutableMap<String, CacheItem> = mutableMapOf()

    private fun <T: Any> cache(name: String, load: (FileObject) -> T): T {
        val fileObject = getFileObject(name)
        if (noCache || !cache.containsKey(name) || (cache[name]!!.fileObject.updateTime < fileObject.updateTime)) {
            cache[name] = CacheItem(load(fileObject), fileObject)
        }
        @Suppress("UNCHECKED_CAST")
        return cache[name]!!.item as T
    }

    abstract fun getInputStream(name: String): InputStream

    abstract fun getFileObject(name: String): FileObject

    override fun <T : Any> loadClass(name: String): KClass<T> = if (useScript) {
        cache("$name.kts") {
            logger.info("loading class $name from resource file $name.kts")
            Kotlin.loadClass<T>(InputStreamReader(getInputStream("$name.kts")))
        }
    } else {
        cache("$name.jar") {
            logger.info("loading class $name from resource file $name.jar")
            val buildId = it.metadata?.get("buildId") ?: error("missing buildId meta in $name.jar")
            val jarFile = File(System.getProperty("java.io.tmpdir"), "model.$buildId.jar")
            getInputStream("$name.jar").use { input ->
                FileOutputStream(jarFile).use { output ->
                    input.copyTo(output)
                }
            }
            val classLoader = URLClassLoader(arrayOf(jarFile.toURI().toURL()), this::class.java.classLoader)
            val javaClass = classLoader.loadClass("model.$buildId.Model")
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