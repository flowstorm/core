package com.promethist.core.runtime

import com.promethist.core.resources.FileResource
import com.promethist.core.provider.LocalFileStorage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream

class FileResourceLoader(private val fileResource: FileResource, val basePath: String) : AbstractLoader() {

    override fun getInputStream(name: String): InputStream {
        logger.info("loading file resource $name")
        val path = "$basePath/$name"
        //TODO can be input stream served directly from jax-rs response? can somehow be removed hack to get stream from local file storage?
        return if (fileResource is LocalFileStorage) {
            val buf = ByteArrayOutputStream()
            fileResource.readFile(path, buf)
            ByteArrayInputStream(buf.toByteArray())
        } else {
            ByteArrayInputStream(fileResource.readFile(path).readEntity(ByteArray::class.java))
        }
    }

    override fun getLastModified(name: String): Long {
        logger.info("checking file resource $name")
        return fileResource.getFile("$basePath/$name").updateTime
    }
}