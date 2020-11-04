package com.promethist.core.runtime

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
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
            val friendlyName = catNameById(id.toString())
            return if (friendlyName.isNotEmpty()) friendlyName else name
        }

        companion object {
            val categoriesNames by lazy { RestClient.webTarget("https://filestore.develop.promethist.com/assets/data/wcities-categories.json")
                    .request().get(Dynamic::class.java) }

            fun catNameById(id: String): String {
                for (cat in categoriesNames.list("category")) {
                    if (cat["id"] == id) {
                        return (if (cat.containsKey("singular")) cat["singular"] else cat["name"]) as String
                    }
                    if (cat.containsKey("subcategory")) {
                        for (subcat in cat.list("subcategory")) {
                            if (subcat["id"] == id) {
                                return (if (subcat.containsKey("singular")) subcat["singular"] else subcat["name"]) as String
                            }
                        }
                    }
                }
                return ""
            }
        }
    }

    open class Record(val id: Long, val name: String = "") {

        class Distance(v: String = "0.0") {
            constructor(v: Double = 0.0): this(v.toString())
            val value = v.toDouble()
            val text = when {
                value < 0.1 -> "right here"
                value < 1.0 -> "${(value * 10).toInt() * 100} meters"
                kotlin.math.abs(value - 1.0) < 0.05 -> "${value.toInt()} kilometer"
                kotlin.math.abs(value % 1.0) < 0.05 -> "${value.toInt()} kilometers" // integer values
                value < 1.9 -> "$value kilometers"
                value < 10.0 -> (kotlin.math.round(value * 2) / 2.0).let {
                    if (kotlin.math.abs(it % 1.0) < 0.05) "${it.toInt()} kilometers" else "${it.toInt()} and half kilometers"
                }
                else -> "${value.toInt()} kilometers"
            }
        }
        val distance: Distance = Distance(0.0)
        @JsonProperty("short_desc") val shortDescription: String = ""
        @JsonProperty("long_desc") open val longDescription: String = ""
        @JsonProperty("category") val categories: List<Category> = listOf()
        val rating: Float = 0.0F
        var openingHours: String = ""
        var acceptCards: Boolean = false
        @JsonProperty("average_cost") val averageCost: Float = 0.0F
        var bookingAdvisable: Boolean = false
        var takeawayAvailable: Boolean = false

        @get:JsonIgnore
        val category get() = categories.first()

        @JsonProperty("open_hours")
        private fun extractOpenHours(openHours: String) {
            openingHours = openHours.replace("Mo", "Monday")
                    .replace("Jan to Dec -", "")
                    .replace("Tu", "Tuesday")
                    .replace("We", "Wednesday")
                    .replace("Th", "Thursday")
                    .replace("Fr", "Friday")
                    .replace("Sa", "Saturday")
                    .replace("Su", "Sunday")
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

        val imagefile = ""
        var imageBasePath = "https://port.develop.promethist.com/proxy/"
        val image get() = if (imagefile.isEmpty()) "" else imageBasePath + imagefile
        @JsonProperty("image_attribution")
        private fun imageAttributes(attributes: Map<String, String>) {
            imageBasePath += attributes["high_res_path"]
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

    class Event(id: Long, name: String): Record(id, name) {

        @JsonProperty("dateTime")
        var dateTimes: MutableList<Interval> = mutableListOf()
        @JsonProperty("desc")
        override val longDescription: String = ""
        var price: String = ""

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
            val time = (data.get("event_time")?.asText() ?: "00:00:00") + " Europe/Berlin"
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

    private fun inRadius(path: String, milesRadius: Int, location: Location?, additionalParameters: Map<String, Any>): WebTarget =
            with(dialogue) {
                var target = target.path(path)
                        .queryParam("miles", milesRadius)
                        .queryParam("lat", location?.latitude ?: clientLocation.latitude)
                        .queryParam("lon", location?.longitude ?: clientLocation.longitude)
                additionalParameters.forEach { (t, u) -> target = target.queryParam(t, u) }
                target
            }

    fun nearCity(milesRadius: Int = 20, location: Location? = null, additionalParameters: Map<String, Any> = mapOf()): Dynamic =
        get<Dynamic>(inRadius("/city_api/getNearCity.php", milesRadius, location, additionalParameters))("nearestCity.city") as Dynamic

    fun events(milesRadius: Int = 20, location: Location? = null,additionalParameters: Map<String, Any> = mapOf()): List<Dynamic> = with(dialogue) {
        get<Dynamic>(inRadius("/event_api/getEvents.php", milesRadius, location, additionalParameters)
                .queryParam("tz", context.turn.input.zoneId)).let {
            if (it.containsKey("cityevent.events.event")) it("cityevent.events.event") else listOf<Dynamic>()
        } as List<Dynamic>
    }

    fun records(milesRadius: Int = 20, category: Int = 1, location: Location? = null, additionalParameters: Map<String, Any> = mapOf()) =
            get<Dynamic>(inRadius("/record_api/getRecords.php", milesRadius, location, additionalParameters)
                    .queryParam("cat", category)).let {
                if (it.containsKey("records.record")) it("records.record") else listOf<Dynamic>()
            } as List<Dynamic>

    fun movies(milesRadius: Int = 20, location: Location? = null, additionalParameters: Map<String, Any> = mapOf()) =
        try {
            get<Dynamic>(inRadius("/movies_api/getMovies.php", milesRadius, location, additionalParameters)).let {
                if (it.containsKey("wcitiesmovies.movie")) it("wcitiesmovies.movie") else listOf<Dynamic>()
            } as List<Dynamic>
        } catch (e: ResponseProcessingException) {
            dialogue.logger.warn("Incorrect content type: ${e.message}")
            listOf<Dynamic>()
        }

    fun theaters(milesRadius: Int = 20, location: Location? = null, additionalParameters: Map<String, Any> = mapOf()) =
        get<Dynamic>(inRadius("/movies_api/getTheaters.php", milesRadius, location, additionalParameters)).let {
            if (it.containsKey("wcitiesmovies.theater")) it("wcitiesmovies.theater") else listOf<Dynamic>()
        } as List<Dynamic>

    companion object {
        val mapper = ObjectUtil.defaultMapper.copy().enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        inline fun <reified V> fromDynamic(obj: Dynamic) = mapper.convertValue(obj, V::class.java)
        inline fun <reified V> fromDynamicList(list: List<Dynamic>) = mapper.convertValue(list, V::class.java)
    }
}

val BasicDialogue.wcities get() = DialogueApi.get<WcitiesApi>(this)