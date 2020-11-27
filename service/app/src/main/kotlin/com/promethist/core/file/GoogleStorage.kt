package com.promethist.core.file

import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import com.promethist.common.AppConfig
import com.promethist.core.FileStorage
import com.promethist.core.model.FileObject
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.channels.ReadableByteChannel
import java.nio.channels.WritableByteChannel

class GoogleStorage: FileStorage {

    companion object {
        const val BUFFER_SIZE = 65536
    }

    private val storage = StorageOptions.getDefaultInstance().service // StorageOptions.newBuilder().setProjectId(..).build().service
    private val bucket = "filestore-" + AppConfig.instance["namespace"]

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
        )?: throw FileStorage.NotFoundException("File $path not found in bucket $bucket")
        return FileObject(blob.name, blob.size, blob.contentType
                ?: "application/octet-stream", blob.createTime, blob.updateTime, blob.metadata ?: mapOf())
    }
}