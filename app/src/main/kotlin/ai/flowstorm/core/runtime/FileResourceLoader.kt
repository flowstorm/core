package ai.flowstorm.core.runtime

import ai.flowstorm.core.model.FileObject
import ai.flowstorm.core.storage.FileStorage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import javax.inject.Inject

class FileResourceLoader(
        private val basePath: String,
        override val noCache: Boolean = false,
        override val useScript: Boolean = false) : AbstractLoader(noCache, useScript) {

    @Inject
    lateinit var fileStorage: FileStorage

    override fun getInputStream(name: String): InputStream {
        logger.info("Loading file resource $name")
        val path = "$basePath/$name"
        val buf = ByteArrayOutputStream()
        fileStorage.readFile(path, buf)
        return ByteArrayInputStream(buf.toByteArray())
    }

    override fun getFileObject(name: String): FileObject {
        logger.info("Checking file resource $name")
        return fileStorage.getFile("$basePath/$name")
    }
}