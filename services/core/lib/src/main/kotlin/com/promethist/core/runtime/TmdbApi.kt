package com.promethist.core.runtime

import com.promethist.common.AppConfig
import com.promethist.core.dialogue.BasicDialogue
import com.promethist.core.type.Dynamic
import java.time.LocalDate
import java.util.*

class TmdbApi(dialogue: BasicDialogue) : DialogueApi(dialogue) {
    enum class SearchType {
        MOVIE, TV, PERSON, MULTI;

        override fun toString(): String {
            return super.toString().toLowerCase()
        }
    }

    private val apiKey = AppConfig.instance["tmdb.key"]
    private val target
        get() = target("https://api.themoviedb.org/3")
                .queryParam("api_key", apiKey)
                .queryParam("language", "en-Us")

    fun search(query: String, type: SearchType = SearchType.MULTI)
        = get<Dynamic>(target.path("/search/$type")
                .queryParam("query", query)
                .queryParam("include_adult", false))("results") as List<LinkedHashMap<String, Any>>

    fun credits(movieName: String): Dynamic {
        val movie = search(movieName)[0]
        return get(target.path("/${movie["media_type"]}/${movie["id"]}/credits").queryParam("movie_id", movie["id"]))

    }

    fun personCredits(personName: String): Dynamic {
        val person = search(personName, type = SearchType.PERSON)[0]
        return get(target.path("/person/${person["id"]}/combined_credits").queryParam("person_id", person["id"]))
    }

    fun popularMovies(releasedAfter: LocalDate, releasedBefore: LocalDate = LocalDate.now(), region: String = "US")
        = get<Dynamic>(target.path("/discover/movie")
                .queryParam("region", region)
                .queryParam("sort_by", "popularity.desc")
                .queryParam("primary_release_date.gte", releasedAfter.toString())
                .queryParam("primary_release_date.lte", releasedBefore.toString())
                .queryParam("include_adult", false))("results") as List<LinkedHashMap<String, Any>>

    fun movieTitle(id: Int): String = get<Dynamic>(target.path("/movie/$id").queryParam("include_adult", false))("title") as String

}

val BasicDialogue.tmdb get() = DialogueApi.get<TmdbApi>(this)