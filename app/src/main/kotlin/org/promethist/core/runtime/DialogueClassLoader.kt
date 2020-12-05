package org.promethist.core.runtime

import org.promethist.util.LoggerDelegate

class DialogueClassLoader(parent: ClassLoader) : ClassLoader(parent) {

    private val classes = mutableMapOf<String, Class<*>>()
    private val logger by LoggerDelegate()

    fun loadClass(name: String, byteCode: ByteArray): DialogueClassLoader {
        classes[name] = defineClass(name, byteCode, 0, byteCode.size)
        return this
    }

    override fun findClass(name: String): Class<*> {
        logger.info("find class $name")
        return classes[name] ?: super.findClass(name)
    }
}