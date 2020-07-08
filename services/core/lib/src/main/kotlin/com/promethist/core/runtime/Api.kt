package com.promethist.core.runtime

import com.fasterxml.jackson.core.type.TypeReference
import com.promethist.common.ObjectUtil
import com.promethist.common.RestClient
import com.promethist.core.type.Dynamic
import com.promethist.core.type.PropertyMap
import javax.ws.rs.client.Entity
import javax.ws.rs.client.Invocation
import javax.ws.rs.client.WebTarget

open class Api {

    fun target(targetUrl: String) = RestClient.webTarget(targetUrl)

    fun Invocation.Builder.headers(headers: PropertyMap? = null): Invocation.Builder {
        headers?.forEach {
            header(it.key, it.value)
        }
        return this
    }

    inline fun <reified T : Any> load(name: String) = this::class.java.getResourceAsStream(name).use {
        ObjectUtil.defaultMapper.readValue<T>(it, object : TypeReference<T>() {})
    }

    inline fun <reified T : Any> get(target: WebTarget, headers: PropertyMap? = null) =
            target.request().headers(headers).get(T::class.java)

    inline fun <reified T : Any> put(target: WebTarget, out: Any, headers: PropertyMap? = null) =
            target.request().headers(headers).put(Entity.json(out), T::class.java)

    inline fun <reified T : Any> post(target: WebTarget, out: Any, headers: PropertyMap? = null) =
            target.request().headers(headers).post(Entity.json(out), T::class.java)

    inline fun <reified T : Any> delete(target: WebTarget, headers: PropertyMap? = null) =
            target.request().headers(headers).delete(T::class.java)

    inline fun <reified T : Any> words(word: String, type: String = "") =
            get<Dynamic>(target("https://wordsapiv1.p.rapidapi.com/words/").path("$word/$type"),
                    headers = mapOf(
                            "x-rapidapi-host" to "wordsapiv1.p.rapidapi.com",
                            "x-rapidapi-key" to "859cf47420msh48dc7d97117df51p1127d4jsn4c6bc3f201ea"
                    )
            ).let {
                if (type.isBlank() && it is Dynamic)
                    it
                else
                    it<T>(type) {
                        value
                }
            } as T

    fun words(word: String, type: String) = words<List<String>>(word, type)
}