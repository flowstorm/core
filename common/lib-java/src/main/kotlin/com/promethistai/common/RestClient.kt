package com.promethistai.common

import com.google.gson.GsonBuilder
import org.glassfish.jersey.client.proxy.WebResourceFactory
import org.glassfish.jersey.jackson.JacksonFeature
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.Serializable
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import javax.ws.rs.client.ClientBuilder

object RestClient {

    fun <I>instance(iface: Class<I>, targetUrl: String): I {
        val target = ClientBuilder.newClient()
                .target(targetUrl)
                .register(JacksonFeature::class)

        return WebResourceFactory.newResource(iface, target)
    }

    fun <T>call(url: URL, responseType: Class<T>, method: String = "GET", output: Any? = null): T {
        val gson = GsonBuilder().create()
        val conn = url.openConnection() as HttpsURLConnection
        conn.readTimeout = 10000
        conn.connectTimeout = 15000
        conn.requestMethod = method
        conn.doInput = true
        conn.doOutput = (output != null)
        conn.setRequestProperty("Content-Type", "application/json")
        conn.connect()
        if (output != null) OutputStreamWriter(conn.outputStream).use {
            gson.toJson(output, it)
        }
        conn.inputStream.use {
            return gson.fromJson<T>(InputStreamReader(it), responseType)
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val res = call(
                URL("https://datastore.develop.promethist.ai/port/contract?key=AIzaSyDpYmTgXGmZY-vWO6ryOcSQ5YZhBsu6NWc"),
                List::class.java,
                "PUT",
                mapOf<String, Serializable>("key" to "AIzaSyDgHsjHyK4cS11nEUJuRGeVUEDITi6OtZA"))
        println(res)
    }
}