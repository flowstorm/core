package org.promethist.core.storage

import org.promethist.core.model.FileObject
import java.io.InputStream
import java.io.OutputStream

interface FileStorage {

    companion object {
        const val defaultContentType = "application/octet-stream"
    }

    class NotFoundException(message: String) : Exception(message)

    fun readFile(path: String, output: OutputStream)
    fun getFile(path: String): FileObject
    fun writeFile(path: String, contentType: String, meta: List<String>, input: InputStream)
    fun deleteFile(path: String): Boolean
}