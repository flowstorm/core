package com.promethist.core.nlu

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.promethist.common.ObjectUtil
import java.time.ZonedDateTime


class NumericEntity: Entity("TIME", "", 1.0F, "duckling") {
    val start: Int = 0
    val end: Int = 0
    val latent: Boolean = false
    @JsonProperty("body")
    override var text = ""

    open class Value {
        @JsonProperty("values")
        var alternativeValues: List<Value> = listOf()
        val type: String = ""
    }

    @JsonDeserialize(using = TimeDeserializer::class)
    open class Time: Value()
    data class GrainedTime(val value: ZonedDateTime, val grain: String = ""): Time()
    data class Interval(val from: GrainedTime, val to: GrainedTime): Time()

    data class Quantity(val value: Float = 0.0F, val product: String = "", val unit: String = ""): Value()

    @JsonProperty("value")
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "dim", include = JsonTypeInfo.As.EXTERNAL_PROPERTY)
    @JsonSubTypes(value = [
        JsonSubTypes.Type(value = GrainedTime::class, name = "time"),
        JsonSubTypes.Type(value = Quantity::class, name = "quantity")
    ])
    val structuredValue: Value = Value()

    class TimeDeserializer : JsonDeserializer<Time?>() {
        override fun deserialize(jsonParser: JsonParser, deserializationContext: DeserializationContext?): Time {
            val value: JsonNode = ObjectUtil.defaultMapper.readTree(jsonParser)
            return if (value.has("type") && value["type"].asText() == "interval") {
                Interval(ObjectUtil.defaultMapper.treeToValue(value["from"], GrainedTime::class.java),
                         ObjectUtil.defaultMapper.treeToValue(value["to"], GrainedTime::class.java))
            } else {
                GrainedTime(ZonedDateTime.parse(value["value"].asText()), value["grain"].asText())
            }
        }
    }

}