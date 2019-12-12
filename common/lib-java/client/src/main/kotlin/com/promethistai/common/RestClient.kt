package com.promethistai.common

import org.glassfish.jersey.client.proxy.WebResourceFactory
import org.glassfish.jersey.jackson.JacksonFeature
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import javax.ws.rs.client.ClientBuilder

object RestClient {

    val mapper = ObjectUtil.defaultMapper

    fun <I>instance(iface: Class<I>, targetUrl: String): I {
        val target = ClientBuilder.newClient()
                .target(targetUrl)
                .register(JacksonFeature::class)

        return WebResourceFactory.newResource(iface, target)
    }

    fun <T>call(url: URL, responseType: Class<T>, method: String = "GET", headers: Map<String, String>? = null, output: Any? = null): T {
        val conn = url.openConnection() as HttpURLConnection
        conn.readTimeout = 10000
        conn.connectTimeout = 15000
        conn.requestMethod = method
        conn.doInput = true
        conn.doOutput = (output != null)
        conn.setRequestProperty("Content-Type", "application/json")
        if (headers != null)
            for (header in headers.entries)
                conn.setRequestProperty(header.key, header.value)
        conn.connect()
        if (output != null) OutputStreamWriter(conn.outputStream).use {
            mapper.writeValue(it, output)
        }
        conn.inputStream.use {
            return mapper.readValue(it, responseType)
        }
    }
}