package com.promethist.core.runtime

import com.promethist.common.AppConfig
import com.promethist.core.dialogue.BasicDialogue
import com.promethist.core.type.*
import javax.ws.rs.client.ResponseProcessingException
import javax.ws.rs.client.WebTarget

class WcitiesApi(dialogue: BasicDialogue) : DialogueApi(dialogue) {

    private val oauthToken = AppConfig.instance["wcities.token"]
    private val target get() = target("http://dev.wcities.com/V3").queryParam("oauth_token", oauthToken)

    enum class Type { CITY, EVENT, RECORD, MOVIE, THEATRE }

    private fun inRadius(path: String, milesRadius: Int, additionalParameters: Map<String, Any>): WebTarget =
            with(dialogue) {
                val target = target.path(path)
                        .queryParam("miles", milesRadius)
                        .queryParam("lat", clientLocation.latitude)
                        .queryParam("lon", clientLocation.longitude)
                additionalParameters.forEach { (t, u) -> target.queryParam(t, u) }
                target
            }

    fun withCustom(type: Type, resourceMethod: () -> List<Dynamic>): List<Dynamic> {
        val list = DynamicMutableList(resourceMethod.invoke())
        val custom = dialogue.context.session.attributes["wcities"][type.toString()]
        if (custom != null) {
            (custom as MemoryMutableList<Dynamic>).forEach { list.add(it.value) }
        }
        return list
    }

    fun nearCity(milesRadius: Int = 20, additionalParameters: Map<String, Any> = mapOf()): Dynamic =
        get<Dynamic>(inRadius("/city_api/getNearCity.php", milesRadius, additionalParameters))("nearestCity.city") as Dynamic

    fun events(milesRadius: Int = 20, additionalParameters: Map<String, Any> = mapOf()): List<Dynamic> = with(dialogue) {
        get<Dynamic>(inRadius("/event_api/getEvents.php", milesRadius, additionalParameters)
                .queryParam("tz", context.turn.input.zoneId))("cityevent.events.event") as List<Dynamic>
    }

    fun records(milesRadius: Int = 20, category: Int = 1, additionalParameters: Map<String, Any> = mapOf()): List<Dynamic> =
            get<Dynamic>(inRadius("/record_api/getRecords.php", milesRadius, additionalParameters)
                    .queryParam("cat", category))("records.record") as List<Dynamic>

    fun movies(milesRadius: Int = 20, additionalParameters: Map<String, Any> = mapOf()): List<Dynamic> {
        try {
            return get<Dynamic>(inRadius("/movies_api/getMovies.php", milesRadius, additionalParameters))("wcitiesmovies.movie") as List<Dynamic>
        } catch (e: ResponseProcessingException) {
            dialogue.logger.warn("Incorrect content type: ${e.message}")
        }
        return listOf()
    }

    fun theaters(milesRadius: Int = 20, additionalParameters: Map<String, Any> = mapOf()): List<Dynamic> {
        val res = get<Dynamic>(inRadius("/movies_api/getTheaters.php", milesRadius, additionalParameters))
        return if (!res.containsKey("wcitiesmovies.theater")) {
            listOf()
        } else {
            get<Dynamic>(inRadius("/movies_api/getTheaters.php",
                    milesRadius, additionalParameters))("wcitiesmovies.theater") as List<Dynamic>
        }
    }

    fun addMockedData(type: Type = Type.RECORD, vararg data: Dynamic) = with(dialogue) {
        val memoryList = MemoryMutableList(data.map { Memory(it) })
        context.session.attributes["wcities"].put(type.toString(), Memorable.pack(memoryList))
    }
}

val BasicDialogue.wcities get() = DialogueApi.get<WcitiesApi>(this)