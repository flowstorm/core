package com.promethist.core.runtime

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import com.promethist.common.AppConfig
import com.promethist.common.ObjectUtil
import com.promethist.core.dialogue.BasicDialogue
import com.promethist.core.type.*
import java.time.format.DateTimeFormatter
import javax.ws.rs.BadRequestException
import javax.ws.rs.client.WebTarget

class ParkopediaApi(dialogue: BasicDialogue) : DialogueApi(dialogue) {

    private data class Token(val token_type: String, val expires_in: Long, val access_token: String) {
        val created = DateTime.now()
        fun isExpired() = DateTime.now().isAfter(created.plusSeconds(expires_in))
    }

    class ParkingSpaceList: ArrayList<ParkingSpace>()
    data class ParkingSpace(val id: Long,
                            val title: String = "",
                            val type: String = "",
                            val info: String = "",
                            val lat: Float = 0.0F,
                            val lng: Float = 0.0F,
                            val distance: Int = 0,
                            val city: String = "",
                            val company: String = "",
                            val country: String = "",
                            var prices: List<Price> = listOf(),
                            @JsonProperty("rt") val realTime: List<RealTime> = listOf(),
                            val addresses: List<String> = listOf()) {
        val walkingDistance get() = (distance / 1000) * 4 * 60

        @get:JsonIgnore
        val price get() = prices.first()

        @JsonProperty("cprices")
        private fun unpackPrices(data: JsonNode) {
            prices = data.get("items").map { ObjectUtil.defaultMapper.convertValue(it, Price::class.java) }
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
    }

}

val BasicDialogue.parkopedia get() = DialogueApi.get<ParkopediaApi>(this)