package org.promethist.common

import org.glassfish.jersey.client.proxy.WebResourceFactory
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJaxbJsonProvider
import java.io.OutputStreamWriter
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Proxy
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Named
import javax.ws.rs.Path
import javax.ws.rs.WebApplicationException
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.client.WebTarget
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.javaMethod
import org.promethist.common.ObjectUtil.defaultMapper as mapper

object RestClient {

    inline fun <reified I> proxy(obj: I, target: String): I =
        Proxy.newProxyInstance(I::class.java.classLoader, arrayOf<Class<*>>(I::class.java)) { _, method, args ->
            try {
                if (args != null) {
                    method.invoke(obj, *args)
                }                 else {
                    method.invoke(obj)
                }
            } catch (e: Throwable) {
                throw when {
                    e.cause is WebApplicationException -> e.cause!!
                    e is WebApplicationException -> e
                    else -> InvocationTargetException(e, "Call to ${I::class.simpleName}.${method.name} on $target failed"
                            + (e.cause?.let {  " - ${it.message}" }))
                }
            }
        } as I

    inline fun <reified I>instance(iface: Class<I>, targetUrl: String): I {
        val provider = JacksonJaxbJsonProvider()
        provider.setMapper(mapper)
        val target = ClientBuilder.newClient().register(provider).target(targetUrl)
        val resource = WebResourceFactory.newResource(iface, target)
        return proxy<I>(resource, targetUrl)
    }

    fun <T> call(url: URL, responseType: Class<T>, method: String = "GET", headers: Map<String, String>? = null, output: Any? = null, timeout: Int = 30000): T =
        call<Any>(url, method, headers, output, timeout).run {
            inputStream.use {
                mapper.readValue(it, responseType)
            }
        }

    fun <T> call(url: URL, method: String = "GET", headers: Map<String, String>? = null, output: Any? = null, timeout: Int = 30000): HttpURLConnection {
        val conn = url.openConnection() as HttpURLConnection
        conn.readTimeout = timeout
        conn.connectTimeout = 5000
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
        return conn
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