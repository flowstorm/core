package com.promethist.core.storage

import com.promethist.common.AppConfig
import com.promethist.core.model.FileObject
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.core.sync.ResponseTransformer
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.*
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.channels.ReadableByteChannel

class AmazonS3Storage: FileStorage {

    companion object {
        const val BUFFER_SIZE = 65536
    }

    private val s3 = S3Client.builder().build()
    private val bucket = AppConfig.instance["s3bucket"]

    override fun readFile(path: String, output: OutputStream) {
        s3.getObject(GetObjectRequest.builder()
                .bucket(bucket)
                .key(path)
                .build(),
                ResponseTransformer.toOutputStream(output))
    }

    override fun getFile(path: String): FileObject {
        val objectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(path)
                .build()
        try {
            val response = s3.getObject(objectRequest).response()
            val metadata = response.metadata().toMutableMap()
            val timeCreated = metadata.remove("time_created")?.toLong() ?: 0L
            return FileObject(path, response.contentLength(), response.contentType(),
                    timeCreated, response.lastModified().toEpochMilli(), metadata)
        } catch (e: NoSuchKeyException) {
            throw FileStorage.NotFoundException("File $path not found in bucket $bucket")
        }
    }

    override fun writeFile(path: String, contentType: String, meta: List<String>, input: InputStream) {
        val metadata = meta.map { it.split(":").let { it[0] to it[1] } }.toMap()
        // First create a multipart upload and get the upload id
        val createMultipartUploadRequest = CreateMultipartUploadRequest.builder()
                .bucket(bucket)
                .key(path)
                .metadata(metadata)
                .contentType(contentType)
                .metadata(mapOf("time_created" to System.currentTimeMillis().toString())) // Different from lastModified even for the first revision!
                .build()

        val response = s3.createMultipartUpload(createMultipartUploadRequest)
        val uploadId = response.uploadId()

        val reader: ReadableByteChannel = Channels.newChannel(input)
        val buffer: ByteBuffer = ByteBuffer.allocate(BUFFER_SIZE)
        var partIdx = 1
        val uploadedParts = mutableListOf<CompletedPart>()
        // Upload all the different parts of the object
        while (reader.read(buffer) > 0) {
            buffer.flip()
            val uploadPartRequest = UploadPartRequest.builder()
                    .bucket(bucket)
                    .key(path)
                    .uploadId(uploadId)
                    .partNumber(partIdx)
                    .build()
            val etag = s3.uploadPart(uploadPartRequest, RequestBody.fromByteBuffer(buffer)).eTag()
            uploadedParts.add(CompletedPart.builder().partNumber(partIdx).eTag(etag).build())
            partIdx++
            buffer.clear()
        }
        // Finally call completeMultipartUpload operation to tell S3 to merge all uploaded
        // parts and finish the multipart operation.
        val completedMultipartUpload = CompletedMultipartUpload.builder()
                .parts(uploadedParts).build()

        val completeMultipartUploadRequest: CompleteMultipartUploadRequest = CompleteMultipartUploadRequest.builder()
                .bucket(bucket)
                .key(path)
                .uploadId(uploadId)
                .multipartUpload(completedMultipartUpload)
                .build()

        s3.completeMultipartUpload(completeMultipartUploadRequest)
    }

    override fun deleteFile(path: String): Boolean {
        val deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(path)
                .build()

        s3.deleteObject(deleteObjectRequest)
        return true
    }
}