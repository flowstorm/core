package ai.flowstorm.core.runtime

import ai.flowstorm.util.LoggerDelegate
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URLClassLoader
import kotlin.jvm.internal.Reflection
import kotlin.reflect.KClass

class DialogueClassLoader(parent: ClassLoader) : ClassLoader(parent) {

    private val classes = mutableMapOf<String, Class<*>>()
    private val logger by LoggerDelegate()

    fun loadClass(name: String, byteCode: ByteArray): DialogueClassLoader {
        classes[name] = defineClass(name, byteCode, 0, byteCode.size)
        return this
    }

    override fun findClass(name: String): Class<*> {
        logger.info("Finding class $name")
        return classes[name] ?: super.findClass(name)
    }


    companion object {

        fun <T : Any> loadClass(parent: ClassLoader, jar: InputStream, buildId: String): KClass<T> {
            //FIXME security -> can we limit visibility scope of dialogue classes to particular set of packages here?
            val jarFile = File(System.getProperty("java.io.tmpdir"), "model.$buildId.jar")
            jar.use { input ->
                FileOutputStream(jarFile).use { output ->
                    input.copyTo(output)
                }
            }
            val classLoader = URLClassLoader(arrayOf(jarFile.toURI().toURL()), parent)
            val javaClass = classLoader.loadClass("model.$buildId.Model")
            return Reflection.createKotlinClass(javaClass) as KClass<T>
        }
    }
}