package com.promethist.core.runtime

import com.promethist.core.dialogue.BasicDialogue
import com.promethist.core.type.Dynamic
import javax.ws.rs.client.WebTarget

class WcitiesApi(dialogue: BasicDialogue) : DialogueApi(dialogue) {

    private val oauthToken = "<DUMMY_TOKEN>"
    private val target get() = target("http://dev.wcities.com/V3").queryParam("oauth_token", oauthToken)

    // Local API mockup. Remove to use the actual API endpoint
    private inline fun <reified T : Any> get(target: WebTarget): T {
        val path = "/test/wcities" + target.uri.path
                .replace("/V3", "")
                .replace(".php", "") + ".json"
        return load(path)
    }


    private fun inRadius(path: String, milesRadius: Int, additionalParameters: Map<String, Any>): WebTarget =
            with(dialogue) {
                val target = target.path(path)
                        .queryParam("miles", milesRadius)
                        .queryParam("lat", clientLocation.latitude)
                        .queryParam("lon", clientLocation.longitude)
                additionalParameters.forEach { (t, u) -> target.queryParam(t, u) }
                target
            }

    fun nearCities(milesRadius: Int = 20, additionalParameters: Map<String, Any> = mapOf()): Dynamic =
            get(inRadius("/city_api/getNearCity.php", milesRadius, additionalParameters))

    fun events(milesRadius: Int = 20, additionalParameters: Map<String, Any> = mapOf()): Dynamic = with(dialogue) {
        get(inRadius("/event_api/getEvents.php", milesRadius, additionalParameters)
                .queryParam("tz", context.turn.input.zoneId))
    }

    fun poi(milesRadius: Int = 20, category: Int, additionalParameters: Map<String, Any> = mapOf()): Dynamic =
            get(inRadius("/record_api/getRecords.php", milesRadius, additionalParameters)
                    .queryParam("cat", category))

    fun movies(milesRadius: Int = 20, additionalParameters: Map<String, Any> = mapOf()): Dynamic =
            get(inRadius("/movies_api/getMovies.php", milesRadius, additionalParameters))

    fun theaters(milesRadius: Int = 20, additionalParameters: Map<String, Any> = mapOf()): Dynamic =
            get(inRadius("/theater_api/getTheaters.php", milesRadius, additionalParameters))
}

val BasicDialogue.wcities get() = DialogueApi.get<WcitiesApi>(this)