package com.promethist.client.util

import com.promethist.client.HttpRequest
import com.promethist.util.LoggerDelegate
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.io.File

object HttpUtil {

    private val httpClient = OkHttpClient()
    private val tmpDir = File(System.getProperty("java.io.tmpdir"))
    private val logger by LoggerDelegate()

    fun httpRequest(url: String, httpRequest: HttpRequest? = null, cache: Boolean = true): ByteArray? {
        val tmpFile = File(tmpDir, "http-cache" + url.hashCode().toString() + ".bin")
        if (((httpRequest == null) || (httpRequest.method == "GET") && cache) && tmpFile.exists()) {
            logger.debug("httpRequest HIT $tmpFile")
            return tmpFile.readBytes()
        } else {
            val builder = Request.Builder().url(url)
            if (httpRequest != null) {
                when (httpRequest.method) {
                    "POST" -> builder.post(RequestBody.create(httpRequest.contentType.toMediaType(), httpRequest.body))
                    "PUT" -> builder.put(RequestBody.create(httpRequest.contentType.toMediaType(), httpRequest.body))
                    "DELETE" -> builder.delete()
                }
                for (header in httpRequest.headers.entries)
                    builder.addHeader(header.key, header.value)
            }
            val request = builder.build()
            val response = httpClient.newCall(request).execute()
            val data = response.body?.byteStream()?.readBytes()
            if ((data != null) && (request == null) || (request.method == "GET" && cache)) {
                logger.debug("httpRequest SAVE $tmpFile")
                tmpFile.writeBytes(data!!)
            }
            return data
        }
    }
}