package ai.flowstorm.client.util

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import ai.flowstorm.client.HttpRequest
import ai.flowstorm.security.X509TrustAllManager
import ai.flowstorm.util.LoggerDelegate
import java.io.File
import java.io.InputStream
import java.util.concurrent.TimeUnit

object HttpUtil {

    val builder = OkHttpClient.Builder().apply {
        val trust = System.getProperty("ai.flowstorm.ssl.trust")
        if (trust == "all") {
            System.err.println("SSL trust set to $trust")
            sslSocketFactory(X509TrustAllManager.sslSocketFactory, X509TrustAllManager)
            hostnameVerifier { _, _ -> true }
        }
        pingInterval(System.getProperty("ai.flowstorm.http.pingInterval")?.toLong() ?: 10, TimeUnit.SECONDS)
    }
    private val tmpDir = File(System.getProperty("java.io.tmpdir"))
    private val logger by LoggerDelegate()

    fun httpRequestStream(url: String, httpRequest: HttpRequest? = null, raiseExceptions: Boolean = false): InputStream? {
        val requestBuilder = Request.Builder().url(url)
        if (httpRequest != null) {
            when (httpRequest.method) {
                "POST" -> requestBuilder.post(RequestBody.create(httpRequest.contentType.toMediaType(), httpRequest.body))
                "PUT" -> requestBuilder.put(RequestBody.create(httpRequest.contentType.toMediaType(), httpRequest.body))
                "DELETE" -> requestBuilder.delete()
            }
            for (header in httpRequest.headers.entries)
                requestBuilder.addHeader(header.key, header.value)
        }
        val request = requestBuilder.build()
        val response = builder.build().newCall(request).execute()
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