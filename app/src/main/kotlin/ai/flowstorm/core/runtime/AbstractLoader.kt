package ai.flowstorm.core.runtime

import com.fasterxml.jackson.core.type.TypeReference
import ai.flowstorm.core.model.FileObject
import ai.flowstorm.core.type.PropertyMap
import ai.flowstorm.util.LoggerDelegate
import java.io.InputStream
import java.io.InputStreamReader
import kotlin.reflect.KClass
import ai.flowstorm.common.ObjectUtil.defaultMapper as mapper

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

    override fun <T : Any> loadClass(className: String): KClass<T> = if (useScript) {
        cache("$className.kts") {
            logger.info("Loading class $className from resource file $className.kts")
            Kotlin.loadClass<T>(InputStreamReader(getInputStream("$className.kts")))
        }
    } else {
        cache("$className.jar") {
            logger.info("Loading class $className from resource file $className.jar")
            val buildId = it.metadata?.get("buildId") ?: error("missing buildId meta in $className.jar")
            DialogueClassLoader.loadClass(this::class.java.classLoader, getInputStream("$className.jar"), buildId)
        }
    }

    override fun loadText(name: String): String = cache("$name.txt") {
        logger.info("Loading text $name")
        String(getInputStream("$name.txt").readBytes())
    }

    override fun <T : Any> loadObject(name: String, typeReference: TypeReference<T>): T = cache("$name.json") {
        logger.info("Loading object $name")
        mapper.readValue<T>(getInputStream("$name.json"), typeReference)
    }

    override fun <T : Any> newObject(name: String, vararg args: Any?): T {
        logger.info("Creating object $name ${args.toList()}")
        return Kotlin.newObject(loadClass(name), *args)
    }

    override fun <T : Any> newObjectWithArgs(name: String, args: PropertyMap): T {
        logger.info("Creating object $name ${args.toList()}")
        return Kotlin.newObjectWithArgs(loadClass(name), args)
    }
}