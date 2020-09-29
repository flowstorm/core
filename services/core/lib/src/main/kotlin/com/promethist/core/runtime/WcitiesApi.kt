package com.promethist.core.runtime

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.promethist.common.AppConfig
import com.promethist.common.ObjectUtil
import com.promethist.common.RestClient
import com.promethist.core.dialogue.BasicDialogue
import com.promethist.core.type.*
import java.time.format.DateTimeFormatter
import javax.ws.rs.client.ResponseProcessingException
import javax.ws.rs.client.WebTarget

class WcitiesApi(dialogue: BasicDialogue) : DialogueApi(dialogue) {

    private val oauthToken = AppConfig.instance["wcities.token"]
    private val target get() = target("http://dev.wcities.com/V3").queryParam("oauth_token", oauthToken)

    data class Category(val cat: Int, val id: Int, @JsonProperty("catname") val name: String) {
        override fun toString(): String {
            return catNameById(id.toString())
        }

        companion object {
            val categoriesNames by lazy { mapper.readValue<Dynamic>(WcitiesApi::class.java.getResourceAsStream("wcities-categories.json"),
                    object : TypeReference<Dynamic>(){}) }

            fun catNameById(id: String): String {
                for (cat in categoriesNames.list("eventCategories.category")) {
                    if (cat["id"] == id) {
                        return cat["name"] as String
                    }
                    if (cat.containsKey("subcategory")) {
                        for (subcat in cat.list("subcategory")) {
                            if (subcat["id"] == id) {
                                return subcat["name"] as String
                            }
                        }
                    }
                }
                return ""
            }
        }
    }

    data class Record(val id: Long,
                      val name: String = "",
                      @JsonProperty("short_desc") val shortDescription: String = "",
                      @JsonProperty("long_desc") val longDescription: String = "",
                      @JsonProperty("category") val categories: List<Category> = listOf(),
                      val rating: Float = 0.0F,
                      var openingHours: String = "",
                      var acceptCards: Boolean = false, 
                      @JsonProperty("average_cost") val averageCost: Int = 0,
                      var bookingAdvisable: Boolean = false,
                      var takeawayAvailable: Boolean = false) {
        @JsonProperty("open_hours")
        private fun extractOpenHours(openHours: String) {
            openingHours = openHours.replace("Mo", "Mon")
                    .replace("Jan to Dec -", "")
                    .replace("Tu", "Tue")
                    .replace("We", "Wed")
                    .replace("Th", "Thu")
                    .replace("Fr", "Fri")
                    .replace("Sa", "Sat")
                    .replace("Su", "Sun")
                    .replace(",", ", ")
                    .replace("Jan", "January").replace("Feb", "February")
                    .replace("Mar", "March").replace("Apr", "April")
                    .replace("Jun", "June").replace("Jul", "July")
                    .replace("Aug", "August").replace("Sep", "September")
                    .replace("Oct", "October").replace("Nov", "November")
                    .replace("Dec", "December")
        }
        @JsonProperty("credit_card")
        private fun extractOpenHours(cards: List<Map<String, String>>) {
            acceptCards = cards.isNotEmpty()
        }

        @JsonProperty("details")
        private fun details(details: JsonNode) {
            mapper.readerForUpdating(this).readValue<Record>(details)
        }

        @JsonProperty("booking_advisable")
        private fun booking(booking: String) {
            bookingAdvisable = "1" == booking
        }

        @JsonProperty("takeaway_available")
        private fun takeaway(takeaway: String) {
            takeawayAvailable = "1" == takeaway
        }
    }

    data class Event(val id: Long,
                     val name: String,
                     val distance: Double = 0.0,
                     @JsonProperty("category") val categories: List<Category> = listOf(),
                     @JsonProperty("dateTime") var dateTimes: MutableList<Interval> = mutableListOf(),
                     var price: String = "") {

        @get:JsonIgnore
        val category get() = categories.first()

        @get:JsonIgnore
        val dateTime get() = dateTimes.first()


        data class Interval(val start: DateTime? = null, val end:DateTime? = null)

        private var venueId = 0
        val venue by lazy {
            val res = RestClient.webTarget("http://dev.wcities.com/V3/record_api/getRecords.php?id=$venueId")
                    .queryParam("oauth_token", AppConfig.instance["wcities.token"]).request().get(Dynamic::class.java)("records.record.details")
            mapper.convertValue(res, Record::class.java)
        }

        @JsonProperty("schedule")
        private fun unpackSchedule(data: JsonNode) {
            venueId = data["venue_id"].asInt()
            val time = (data.get("event_time")?.asText() ?: "00:00:00") + " +02:00"
            if (!data["dates"].isArray) {
                dateTimes.add(Interval(DateTime.parse("${data["dates"]["start"].asText()} $time", DATE_FORMATTER),
                        DateTime.parse("${data["dates"]["end"].asText()} $time", DATE_FORMATTER)))
            } else {
                data.withArray<JsonNode>("dates").forEach {
                    dateTimes.add(Interval(DateTime.parse("${it["start"].asText()} $time", DATE_FORMATTER),
                            DateTime.parse("${it["end"].asText()} $time", DATE_FORMATTER)))
                }
            }

            val priceNode = data.get("bookinginfo")?.get("bookinglink")?.get("price")
            price = "${(priceNode?.get("value")?.asText() ?: "").replace(",",".")} ${priceNode?.get("currency")?.asText() ?: ""}".trim()
        }

        companion object {
            val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z")
        }
    }

    class EventList: ArrayList<Event>()
    class RecordList: ArrayList<Record>()

    private fun inRadius(path: String, milesRadius: Int, additionalParameters: Map<String, Any>): WebTarget =
            with(dialogue) {
                var target = target.path(path)
                        .queryParam("miles", milesRadius)
                        .queryParam("lat", clientLocation.latitude)
                        .queryParam("lon", clientLocation.longitude)
                additionalParameters.forEach { (t, u) -> target = target.queryParam(t, u) }
                target
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

    companion object {
        val mapper = ObjectUtil.defaultMapper.copy().enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    }
}

val BasicDialogue.wcities get() = DialogueApi.get<WcitiesApi>(this)