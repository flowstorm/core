package org.promethist.core.runtime

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import org.promethist.common.AppConfig
import org.promethist.common.ObjectUtil
import org.promethist.core.dialogue.BasicDialogue
import org.promethist.core.type.DateTime
import org.promethist.core.type.Dynamic
import org.promethist.core.type.value.Amount
import org.promethist.core.type.value.Duration
import java.time.format.DateTimeFormatter
import javax.ws.rs.BadRequestException
import javax.ws.rs.client.WebTarget

class ParkopediaApi(dialogue: BasicDialogue) : DialogueApi(dialogue) {

    private data class Token(val token_type: String, val expires_in: Long, val access_token: String) {
        val created = DateTime.now()
        fun isExpired() = DateTime.now().isAfter(created.plusSeconds(expires_in))
    }

    class ParkingSpaceList: ArrayList<ParkingSpace>()
    class ParkingSpace(val id: Long,
                        val title: String = "",
                        val type: String = "",
                        val info: String = "",
                        val lat: Float = 0.0F,
                        val lng: Float = 0.0F,
                        val distance: Int = 0,
                        val city: String = "",
                        val company: String = "",
                        val country: String = "",
                        @JsonProperty("heightrestricted")val heightRestricted: Boolean = false,
                        val height: Int = 999,
                        var prices: List<Price> = listOf(Price(error = "no price")),
                        @JsonProperty("rt") val realTime: List<RealTime> = listOf(),
                        val addresses: List<String> = listOf(),
                            surface: Int = 0,
                            ctype: Int = 0) {

        val surface: String = when (surface) {
            1 -> "multistorey"
            2 -> "not covered"
            3 -> "covered"
            4 -> "underground"
            5 -> "partially covered"
            6 -> "mechanical"
            else -> "unknown"
        }
        val payByCash = ctype.and(1 shl 0) > 0 || ctype.and(1 shl 1) > 0
        val payByCard = ctype.and(1 shl 2) > 0 || ctype.and(1 shl 4) > 0 || ctype.and(1 shl 5) > 0
        val openingTimes = "nonstop"

        val walkingDistance get() = distance * 60 / 1000 / 4
        val name get() = title.replace(" *\\d+$".toRegex(), "")
        var durationRestricted = false
        var durationMax = Duration(Float.MAX_VALUE, "day")

        @get:JsonIgnore
        val price get() = prices.first()

        @JsonProperty("cprices")
        private fun unpackPrices(data: JsonNode) {
            prices = data.get("items").map { ObjectUtil.defaultMapper.convertValue(it, Price::class.java) }
        }

        @JsonProperty("priceschema")
        private fun unpackRestrictions(data: JsonNode) {
            if (data.has("prices") && !data["prices"].has(0)) {
                durationRestricted = data["prices"][0].has("maxstay_mins") && data["prices"][0]["maxstay_mins"].asInt() > 0
                if (durationRestricted) {
                    val minutes = data["prices"][0]["maxstay_mins"].asInt()
                    val normalized = Amount(minutes.toBigDecimal(), "minute")
                    durationMax = when {
                        minutes <= 120 -> Duration(minutes.toFloat(), "minute", normalized = normalized)
                        minutes <= 48 * 60 -> Duration((minutes / 60).toFloat(), "hour", normalized = normalized)
                        else -> Duration((minutes / 60 / 24).toFloat(), "day", normalized = normalized)
                    }
                }
            }
        }
    }

    data class Price(val date: String = "", val amount: Int = 0, val text: String = "0.00 €", val rounded: String = "0 €", val error: String = "")
    data class RealTime(val trend: Int = 0, val state: Int = 0, val indicator: String = "")

    private val clientId = AppConfig.instance["parkopedia.client-id"]
    private val clientSecret = AppConfig.instance["parkopedia.client-secret"]
    private val target get() = target(AppConfig.instance["parkopedia.url"]).queryParam("apiver", 35)
    private var token: Token? = null
    private val headers get() = mapOf("Authorization" to "Bearer ${token?.access_token}")

    private fun inRadius(path: String, radius: Int): WebTarget =
            with(dialogue) {
                val target = target.path(path)
                        .queryParam("radius", radius)
                        .queryParam("lat", clientLocation.latitude)
                        .queryParam("lng", clientLocation.longitude)
                target
            }

    private fun createToken() = post<Token>(target.path("tokens"), mapOf("client_id" to clientId,
            "client_secret" to clientSecret, "grant_type" to "client_credentials"))

    private fun refreshToken() {
        token = if (token == null || token!!.isExpired()) createToken() else token
    }

    fun search(radius: Int = 500, sort: String = "distance", from: DateTime? = null, to: DateTime? = null): List<Dynamic> {
        refreshToken()
        var target = inRadius("/search", radius).queryParam("cid", clientId).queryParam("sort", sort)

        if (from != null && to != null) {
            target = target.queryParam("dates", "${from.format(DATE_FORMATTER)}-${to.format(DATE_FORMATTER)}")
        }
        return try {
            get<Dynamic>(target, headers).list("result.spaces")
        } catch (e: BadRequestException) {
            listOf()
        }
    }

    fun search(radius: Int = 500, sort: String = "distance", duration: Int, from: DateTime = DateTime.now()) = search(radius, sort, from, from.plusMinutes(duration.toLong()))

    companion object {
        val DATE_FORMATTER = DateTimeFormatter.ofPattern("YYYYMMddHHmm")
        fun fromDynamic(obj: Dynamic) = ObjectUtil.defaultMapper.convertValue(obj, ParkingSpace::class.java)
        fun fromDynamicList(list: List<Dynamic>) = ObjectUtil.defaultMapper.convertValue(list, ParkingSpaceList::class.java)
    }

}

val BasicDialogue.parkopedia get() = DialogueApi.get<ParkopediaApi>(this)