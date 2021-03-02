package ai.flowstorm.core.runtime

import ai.flowstorm.util.LoggerDelegate
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URLClassLoader
import kotlin.jvm.internal.Reflection
import kotlin.reflect.KClass

class DialogueClassLoader(parent: ClassLoader) : ClassLoader(parent) {

    private val logger by LoggerDelegate()

    override fun loadClass(name: String): Class<*> {
        logger.debug("Loading class $name")
        return super.loadClass(name)
    }

    override fun findClass(name: String): Class<*> {
        logger.debug("Finding class $name")
        return super.findClass(name)
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
            val classLoader = URLClassLoader(arrayOf(jarFile.toURI().toURL()), DialogueClassLoader(parent))
            val javaClass = classLoader.loadClass("model.$buildId.Model")
            return Reflection.createKotlinClass(javaClass) as KClass<T>
        }
    }
}