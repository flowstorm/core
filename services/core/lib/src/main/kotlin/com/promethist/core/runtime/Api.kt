package com.promethist.core.runtime

import com.promethist.common.RestClient
import com.promethist.core.type.Dynamic
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class Api {

    inline fun <reified T : Any> get(url: String, headers: Map<String, String>? = null) =
            RestClient.call(URL(url), T::class.java, "GET", headers)

    inline fun <reified T : Any> put(url: String, out: Any, headers: Map<String, String>? = null) =
            RestClient.call(URL(url), T::class.java, "PUT", headers, out)

    inline fun <reified T : Any> post(url: String, out: Any, headers: Map<String, String>? = null) =
            RestClient.call(URL(url), T::class.java, "POST", headers, out)

    inline fun <reified T : Any> words(word: String, what: String = "") =
            get<Dynamic>("https://wordsapiv1.p.rapidapi.com/words/" + URLEncoder.encode(word, StandardCharsets.UTF_8.toString()) + "/$what",
                    headers = mapOf(
                            "x-rapidapi-host" to "wordsapiv1.p.rapidapi.com",
                            "x-rapidapi-key" to "859cf47420msh48dc7d97117df51p1127d4jsn4c6bc3f201ea"
                    )
            ).let {
                if (what.isBlank() && it is Dynamic)
                    it
                else
                    it<T>(what) {
                        value
                }
            } as T
}