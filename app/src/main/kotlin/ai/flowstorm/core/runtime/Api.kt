package ai.flowstorm.core.runtime

import com.fasterxml.jackson.core.type.TypeReference
import ai.flowstorm.common.ObjectUtil
import ai.flowstorm.common.RestClient
import ai.flowstorm.core.type.Dynamic
import ai.flowstorm.core.type.PropertyMap
import java.net.URLEncoder
import javax.ws.rs.client.Entity
import javax.ws.rs.client.Invocation
import javax.ws.rs.client.WebTarget

open class Api {

    @Deprecated("Obsolete", ReplaceWith("pass URL as String instead of WebTarget to API methods"))
    fun target(targetUrl: String) = RestClient.webTarget(targetUrl)

    fun encode(text: String): String = URLEncoder.encode(text, "utf-8")

    private fun Invocation.Builder.headers(headers: PropertyMap? = null): Invocation.Builder {
        headers?.forEach {
            header(it.key, it.value)
        }
        return this
    }

    inline fun <reified T : Any> load(name: String): T = this::class.java.getResourceAsStream(name).use {
        ObjectUtil.defaultMapper.readValue<T>(it, object : TypeReference<T>() {})
    }

    @Deprecated("Obsolete", ReplaceWith("get(targetUrl, headers = null)"))
    inline fun <reified T : Any> get(target: WebTarget, headers: PropertyMap? = null): T = invoke(target, headers) {
        get(T::class.java)
    }

    inline fun <reified T : Any> get(targetUrl: String, headers: PropertyMap? = null): T = get(T::class.java, targetUrl, headers)

    fun <T : Any> get(returnType: Class<T>, targetUrl: String, headers: PropertyMap? = null): T = invoke(targetUrl, headers) {
        get(returnType)
    }

    @Deprecated("Obsolete", ReplaceWith("put(targetUrl, headers = null)"))
    inline fun <reified T : Any> put(target: WebTarget, out: Any, headers: PropertyMap? = null): T = invoke(target, headers) {
        put(Entity.json(out), T::class.java)
    }

    inline fun <reified T : Any> put(targetUrl: String, out: Any, headers: PropertyMap? = null): T = put(T::class.java, targetUrl, out, headers)

    fun <T : Any> put(returnType: Class<T>, targetUrl: String, out: Any, headers: PropertyMap? = null): T = invoke(targetUrl, headers) {
        put(Entity.json(out), returnType)
    }

    @Deprecated("Obsolete", ReplaceWith("post(targetUrl, headers = null)"))
    inline fun <reified T : Any> post(target: WebTarget, out: Any, headers: PropertyMap? = null): T = invoke(target, headers) {
        post(Entity.json(out), T::class.java)
    }

    inline fun <reified T : Any> post(targetUrl: String, out: Any, headers: PropertyMap? = null): T = post(T::class.java, targetUrl, out, headers)

    fun <T : Any> post(returnType: Class<T>, targetUrl: String, out: Any, headers: PropertyMap? = null): T = invoke(targetUrl, headers) {
        post(Entity.json(out), returnType)
    }

    @Deprecated("Obsolete", ReplaceWith("delete(targetUrl, headers = null)"))
    inline fun <reified T : Any> delete(target: WebTarget, headers: PropertyMap? = null): T = invoke(target, headers) {
        delete(T::class.java)
    }

    inline fun <reified T : Any> delete(targetUrl: String, headers: PropertyMap? = null): T = delete(T::class.java, targetUrl, headers)

    fun <T : Any> delete(returnType: Class<T>, targetUrl: String, headers: PropertyMap? = null): T = invoke(targetUrl, headers) {
        delete(returnType)
    }

    fun <T : Any> invoke(target: WebTarget, headers: PropertyMap? = null, block: Invocation.Builder.() -> T): T {
        val time = System.currentTimeMillis()
        val result = block(target.request().headers(headers))
        val duration = System.currentTimeMillis() - time
        DialogueRuntime.ifRunning {
            context.logger.info("API call to ${target.uri} took $duration ms")
        }
        return result
    }

    fun <T : Any> invoke(targetUrl: String, headers: PropertyMap? = null, block: Invocation.Builder.() -> T): T =
        invoke(target(targetUrl), headers, block)

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