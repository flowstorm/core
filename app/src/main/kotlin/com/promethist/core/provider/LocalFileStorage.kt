package com.promethist.core.provider

import com.promethist.core.model.FileObject
import com.promethist.core.resources.FileResource
import java.io.*
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import javax.ws.rs.NotFoundException
import javax.ws.rs.WebApplicationException
import javax.ws.rs.core.Response
import javax.ws.rs.core.StreamingOutput

const val defaultContentType = "application/octet-stream"

class LocalFileStorage(private val base: File) : FileResource {

    override fun readFile(path: String): Response {
        val fileObject = getFile(path)
        return Response.ok(
                StreamingOutput { output ->
                    try {
                        readFile(path, output)
                    } catch (e: Exception) {
                        throw WebApplicationException("File streaming failed", e)
                    }
                }, defaultContentType)
                .header("Content-Disposition", "inline" + "; filename=\"${fileObject.name}\"")
                .header("Content-Length", fileObject.size)
                .build()
    }

    override fun readFile(path: String, output: OutputStream) {
        val file = File(base, path)
        if (!file.exists())
            throw NotFoundException("File $path not found")
        FileInputStream(file).copyTo(output)
    }

    override fun getFile(path: String): FileObject {
        val file = File(base, path)
        if (file.exists()) {
            val attr = Files.readAttributes(file.toPath(),  BasicFileAttributes::class.java)
            return FileObject(file.name, file.length(), defaultContentType,
                    attr.creationTime().toMillis(), attr.lastModifiedTime().toMillis())
        } else {
            throw NotFoundException("File $path not found")
        }
    }

    override fun writeFile(path: String, contentType: String, meta: List<String>, input: InputStream) {
        val file = File(base, path)
        file.parentFile.mkdirs()
        FileOutputStream(file).use { input.copyTo(it) }
    }

    override fun deleteFile(path: String) = File(base, path).delete()

    override fun provider(): String = "local"

    override fun toString(): String = "${this::class.simpleName}(base=$base)"

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val fileResource = LocalFileStorage(File("test/dialogue"))
            val path = "product/some-dialogue/1/model.kts"
            println(fileResource.getFile(path))
            val buf = fileResource.readFile(path).readEntity(ByteArray::class.java)
            println(buf)
            //fileResource.readFile(path, System.out)
            //fileResource.writeFile("test.txt", "text/plain", listOf(), "test ${System.currentTimeMillis()}".byteInputStream())
        }
    }
}