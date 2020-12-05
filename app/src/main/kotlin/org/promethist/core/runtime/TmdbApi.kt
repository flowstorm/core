package org.promethist.core.runtime

import org.promethist.common.AppConfig
import org.promethist.core.dialogue.BasicDialogue
import org.promethist.core.type.DateTime
import org.promethist.core.type.Dynamic
import java.time.format.DateTimeFormatter

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
                .queryParam("include_adult", false)).list("results")

    fun details(movieName: String, type: SearchType? = null): Dynamic {
        if (type == SearchType.MULTI) {
            throw IllegalArgumentException("Details search is not supported for type MULTI.")
        }
        val movie = search(movieName).getOrElse(0) { return Dynamic.EMPTY }
        val typeString: String = type?.toString() ?: movie["media_type"].toString()
        return get(target.path("/${typeString}/${movie["id"]}"))
    }

    fun credits(movieName: String): Dynamic {
        val movie = search(movieName).getOrElse(0) { return Dynamic.EMPTY }
        return get(target.path("/${movie["media_type"]}/${movie["id"]}/credits"))
    }

    fun personCredits(personName: String): Dynamic {
        val person = search(personName, type = SearchType.PERSON)[0]
        return get(target.path("/person/${person["id"]}/combined_credits"))
    }

    fun popularMovies(releasedAfter: DateTime, releasedBefore: DateTime = DateTime.now(), region: String = "US")
        = get<Dynamic>(target.path("/discover/movie")
                .queryParam("region", region)
                .queryParam("sort_by", "popularity.desc")
                .queryParam("primary_release_date.gte", releasedAfter.format(DateTimeFormatter.ISO_LOCAL_DATE))
                .queryParam("primary_release_date.lte", releasedBefore.format(DateTimeFormatter.ISO_LOCAL_DATE))
                .queryParam("include_adult", false)).list("results")

    fun movieTitle(id: Int): String = get<Dynamic>(target.path("/movie/$id").queryParam("include_adult", false))("title") as String

}

val BasicDialogue.tmdb get() = DialogueApi.get<TmdbApi>(this)