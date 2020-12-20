package org.promethist.client.util

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.promethist.client.HttpRequest
import org.promethist.util.LoggerDelegate
import java.io.File
import java.io.InputStream

object HttpUtil {

    private val httpClient = OkHttpClient()
    private val tmpDir = File(System.getProperty("java.io.tmpdir"))
    private val logger by LoggerDelegate()

    fun httpRequestStream(url: String, httpRequest: HttpRequest? = null, raiseExceptions: Boolean = false): InputStream? {
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
        if (response.code > 399 && raiseExceptions)
            error("HTTP request failed with result ${response.code} ${response.message}")
        return response.body?.byteStream()
    }

    fun httpRequest(url: String, httpRequest: HttpRequest? = null, cache: Boolean = true): ByteArray? {
        val tmpFile = File(tmpDir, "http-cache" + url.hashCode().toString() + ".bin")
        return if (((httpRequest == null) || (httpRequest.method == "GET") && cache) && tmpFile.exists()) {
            logger.debug("HTT request HIT $tmpFile")
            tmpFile.readBytes()
        } else {
            val data = httpRequestStream(url, httpRequest)?.readBytes()
            if ((data != null) && (httpRequest == null || (httpRequest!!.method == "GET" && cache))) {
                logger.debug("HTTP request SAVE $tmpFile")
                tmpFile.writeBytes(data!!)
            }
            data
        }
    }
}