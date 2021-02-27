package ai.flowstorm.core.provider

import ai.flowstorm.core.model.FileObject
import ai.flowstorm.core.storage.FileStorage
import java.io.*
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes

class LocalFileStorage(private val base: File) : FileStorage {

    override fun readFile(path: String, output: OutputStream) {
        val file = File(base, path)
        if (!file.exists())
            throw FileStorage.NotFoundException("File $path not found")
        FileInputStream(file).copyTo(output)
    }

    override fun getFile(path: String): FileObject {
        val file = File(base, path)
        if (file.exists()) {
            val attr = Files.readAttributes(file.toPath(),  BasicFileAttributes::class.java)
            return FileObject(file.name, file.length(), FileStorage.defaultContentType,
                    attr.creationTime().toMillis(), attr.lastModifiedTime().toMillis())
        } else {
            throw FileStorage.NotFoundException("File $path not found")
        }
    }

    override fun writeFile(path: String, contentType: String, meta: List<String>, input: InputStream) {
        val file = File(base, path)
        file.parentFile.mkdirs()
        FileOutputStream(file).use { input.copyTo(it) }
    }

    override fun deleteFile(path: String) = File(base, path).delete()

    override fun toString(): String = "${this::class.simpleName}(base=$base)"
}