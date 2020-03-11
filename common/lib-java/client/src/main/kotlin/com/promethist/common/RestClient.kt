package com.promethist.common

import org.glassfish.jersey.client.proxy.WebResourceFactory
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJaxbJsonProvider
import java.io.OutputStreamWriter
import java.lang.reflect.AnnotatedElement
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Named
import javax.ws.rs.Path
import javax.ws.rs.WebApplicationException
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.client.WebTarget
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.javaMethod

object RestClient {

    val mapper = ObjectUtil.defaultMapper

    fun <I>instance(iface: Class<I>, targetUrl: String): I {
        val provider = JacksonJaxbJsonProvider()
        provider.setMapper(mapper)

        val target = ClientBuilder.newClient()
                .register(provider)
                .target(targetUrl)

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
        if (conn.responseCode > 399)
            throw WebApplicationException(conn.responseMessage, conn.responseCode)
        conn.inputStream.use {
            return mapper.readValue(it, responseType)
        }
    }

    @Named
    fun webTarget(targetUrl: String): WebTarget {
        val provider = JacksonJaxbJsonProvider()
        provider.setMapper(mapper)

        return ClientBuilder.newClient()
                .register(provider)
                .target(targetUrl)
    }
}

fun WebTarget.resourceMethod(method: KFunction<*>): WebTarget {
    val resourceMethod = method.javaMethod!!
    val resourceClass = resourceMethod.declaringClass

    var wt = this
    wt = addPathFromAnnotation(resourceClass, wt)
    wt = addPathFromAnnotation(resourceMethod, wt)

    return wt
}

private fun addPathFromAnnotation(ae: AnnotatedElement, target: WebTarget): WebTarget {
    val p = ae.getAnnotation(Path::class.java)

    return if (p != null) {
        target.path(p.value)
    } else {
        target
    }
}