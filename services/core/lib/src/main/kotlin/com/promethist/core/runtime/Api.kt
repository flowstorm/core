package com.promethist.core.runtime

import com.promethist.common.RestClient
import com.promethist.core.type.Dynamic
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object Api {

    inline fun <reified T : Any> words(word: String, what: String = "") =
            RestClient.call(URL("https://wordsapiv1.p.rapidapi.com/words/" + URLEncoder.encode(word, StandardCharsets.UTF_8) + "/$what"), Dynamic::class.java,
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