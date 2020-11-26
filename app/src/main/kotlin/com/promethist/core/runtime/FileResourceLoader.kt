package com.promethist.core.runtime

import com.promethist.core.FileStorage
import com.promethist.core.model.FileObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream

class FileResourceLoader(
        private val fileStorage: FileStorage,
        private val basePath: String,
        override val noCache: Boolean = false,
        override val useScript: Boolean = false) : AbstractLoader(noCache, useScript) {

    override fun getInputStream(name: String): InputStream {
        logger.info("loading file resource $name")
        val path = "$basePath/$name"
        val buf = ByteArrayOutputStream()
        fileStorage.readFile(path, buf)
        return ByteArrayInputStream(buf.toByteArray())
    }

    override fun getFileObject(name: String): FileObject {
        logger.info("checking file resource $name")
        return fileStorage.getFile("$basePath/$name")
    }
}