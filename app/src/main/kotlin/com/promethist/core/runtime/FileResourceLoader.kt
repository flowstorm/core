package com.promethist.core.runtime

import com.promethist.core.model.FileObject
import com.promethist.core.resources.FileResource
import com.promethist.core.provider.LocalFileStorage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import javax.ws.rs.WebApplicationException

class FileResourceLoader(
        private val fileResource: FileResource,
        private val basePath: String,
        override val noCache: Boolean = false,
        override val useScript: Boolean = false) : AbstractLoader(noCache, useScript) {

    override fun getInputStream(name: String): InputStream {
        logger.info("loading file resource $name")
        val path = "$basePath/$name"
        //TODO can be input stream served directly from jax-rs response? can somehow be removed hack to get stream from local file storage?
        return if (fileResource is LocalFileStorage) {
            val buf = ByteArrayOutputStream()
            fileResource.readFile(path, buf)
            ByteArrayInputStream(buf.toByteArray())
        } else {
            val res = fileResource.readFile(path)
            if (res.status >= 400)
                throw WebApplicationException(res.status)
            val buf = res.readEntity(ByteArray::class.java)
            ByteArrayInputStream(buf)
        }
    }

    override fun getFileObject(name: String): FileObject {
        logger.info("checking file resource $name")
        return fileResource.getFile("$basePath/$name")
    }
}