package com.promethist.core.file

import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import com.promethist.common.AppConfig
import com.promethist.core.model.FileObject
import com.promethist.core.resources.FileResource
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.channels.ReadableByteChannel
import java.nio.channels.WritableByteChannel
import javax.ws.rs.NotFoundException
import javax.ws.rs.Path
import javax.ws.rs.WebApplicationException
import javax.ws.rs.core.Response
import javax.ws.rs.core.StreamingOutput

@Path("/")
class GoogleStorage: FileResource {

    companion object {
        const val BUFFER_SIZE = 65536
    }

    private val storage = StorageOptions.getDefaultInstance().service // StorageOptions.newBuilder().setProjectId(..).build().service
    private val bucket = "filestore-" + AppConfig.instance["namespace"]

    override fun readFile(path: String): Response {
        val file = getFile(path)
        return Response.ok(
                StreamingOutput { output ->
                    try {
                        readFile(path, output)
                    } catch (e: Exception) {
                        throw WebApplicationException("File streaming failed", e)
                    }
                }, file.contentType)
                .header("Content-Disposition", "inline" + "; filename=\"${file.name}\"")
                .header("Content-Length", file.size)
                .build()
    }

    override fun readFile(path: String, out: OutputStream) {
        storage.reader(bucket, path).use { reader ->
            val writer: WritableByteChannel = Channels.newChannel(out)
            val buffer: ByteBuffer = ByteBuffer.allocate(BUFFER_SIZE)
            while (reader.read(buffer) > 0) {
                buffer.flip()
                writer.write(buffer)
                buffer.clear()
            }
        }
    }

    override fun writeFile(path: String, contentType: String, meta: List<String>, input: InputStream) {
        val metadata = meta.map { it.split(":").let { it[0] to it[1] } }.toMap()
        val blobId = BlobId.of(bucket, path)
        val blobInfo = BlobInfo.newBuilder(blobId).setContentType(contentType).setMetadata(metadata).build()
        storage.writer(blobInfo).use { writer ->
            val reader: ReadableByteChannel = Channels.newChannel(input)
            val buffer: ByteBuffer = ByteBuffer.allocate(BUFFER_SIZE)
            while (reader.read(buffer) > 0) {
                buffer.flip()
                writer.write(buffer)
                buffer.clear()
            }
        }
    }

    override fun deleteFile(path: String) = storage.delete(bucket, path)

    override fun getFile(path: String): FileObject {
        val blob = storage.get(bucket, path, Storage.BlobGetOption.fields(
                Storage.BlobField.NAME,
                Storage.BlobField.SIZE,
                Storage.BlobField.CONTENT_TYPE,
                Storage.BlobField.TIME_CREATED,
                Storage.BlobField.UPDATED,
                Storage.BlobField.METADATA)
        )?: throw NotFoundException("File $path not found")
        return FileObject(blob.name, blob.size, blob.contentType
                ?: "application/octet-stream", blob.createTime, blob.updateTime, blob.metadata ?: mapOf())
    }

    override fun provider() = "Google"

    // This version of the readFile function might be useful in the future when we decide to fix Safari compatibility
    // The range parameter must come from Range request header value
    fun readFileSafari(path: String, range: String): Response {
        val file = getFile(path)
        val rangeBytes = range.split("=")[1]
//        val size = if (rangeBytes === "0-") file.size else 2
        return Response.ok(
                StreamingOutput { output ->
                    try {
                        readFile(path, output)
                    } catch (e: Exception) {
                        throw WebApplicationException("File streaming failed", e)
                    }
                }, file.contentType)
                .header("Content-Disposition", "inline" + "; filename=\"${file.name}\"")
                .header("Content-Length", file.size)
                .header("Accept-Ranges", "bytes")
                .header("Content-Range", "bytes ${rangeBytes}/${file.size}")
                .status(206)
                .build()
    }

}