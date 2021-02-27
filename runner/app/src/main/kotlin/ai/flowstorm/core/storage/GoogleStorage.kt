package ai.flowstorm.core.storage

import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import ai.flowstorm.core.model.FileObject
import ai.flowstorm.util.LoggerDelegate
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.channels.ReadableByteChannel
import java.nio.channels.WritableByteChannel

class GoogleStorage(private val bucket: String): FileStorage {

    companion object {
        const val BUFFER_SIZE = 65536
    }

    private val logger by LoggerDelegate()

    init {
        logger.info("Created with bucket $bucket")
    }

    private val storage = StorageOptions.getDefaultInstance().service // StorageOptions.newBuilder().setProjectId(..).build().service

    override fun readFile(path: String, out: OutputStream) {
        logger.debug("Reading file $path")
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
        logger.debug("Writing file $path of content type $contentType")
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

    override fun deleteFile(path: String): Boolean {
        logger.debug("Deleting file $path")
        return storage.delete(bucket, path)
    }

    override fun getFile(path: String): FileObject {
        logger.debug("Getting file $path")
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