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
import com.promethist.core.type.DateTime


class NumericEntity: Entity("NUMERIC", "", 1.0F, "duckling") {
    val start: Int = 0
    val end: Int = 0
    val latent: Boolean = false
    var dim: String = ""
        set(value) {
            field = value
            className = if (value == "amount-of-money") "MONEY" else value.toUpperCase()
        }

    @JsonProperty("body")
    override var text = ""

    open class Value {
        @JsonProperty("values")
        var alternativeValues: List<Value> = listOf()
        val type: String = ""
    }

    @JsonDeserialize(using = TimeDeserializer::class)
    open class Time: Value()
    data class GrainedTime(val value: DateTime, val grain: String = ""): Time()
    data class Interval(val from: GrainedTime, val to: GrainedTime): Time()

    data class Quantity(val value: Float = 0.0F, val product: String = "", val unit: String = ""): Value()
    data class Unit(val value: Float = 0.0F, val unit: String = ""): Value()
    data class CreditCard(val value: String = "", val issuer: String = ""): Value()
    data class Duration(val value: Float = 0.0F, val unit: String = "", val year: Float = 0.0F, val month: Float = 0.0F,
                        val week: Float = 0.0F, val day: Float = 0.0F, val hour: Float = 0.0F, val minute: Float = 0.0F,
                        val second: Float = 0.0F, val normalized: Unit = Unit(0.0F, "second")): Value()
    data class StringValue(val value: String = ""): Value()
    data class NumericValue(val value: Float = 0.0F): Value()
    data class URL(val value: String = "", val domain: String = ""): Value()

    @JsonProperty("value")
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "dim", include = JsonTypeInfo.As.EXTERNAL_PROPERTY, visible = true)
    @JsonSubTypes(value = [
        JsonSubTypes.Type(value = GrainedTime::class, name = "time"),
        JsonSubTypes.Type(value = Quantity::class, name = "quantity"),
        JsonSubTypes.Type(value = Unit::class, name = "amount-of-money"),
        JsonSubTypes.Type(value = Unit::class, name = "distance"),
        JsonSubTypes.Type(value = Unit::class, name = "temperature"),
        JsonSubTypes.Type(value = Unit::class, name = "volume"),
        JsonSubTypes.Type(value = CreditCard::class, name = "credit-card-number"),
        JsonSubTypes.Type(value = Duration::class, name = "duration"),
        JsonSubTypes.Type(value = NumericValue::class, name = "number"),
        JsonSubTypes.Type(value = NumericValue::class, name = "ordinal"),
        JsonSubTypes.Type(value = StringValue::class, name = "email"),
        JsonSubTypes.Type(value = StringValue::class, name = "phone-number"),
        JsonSubTypes.Type(value = URL::class, name = "url")
    ])
    val structuredValue: Value = Value()

    class TimeDeserializer : JsonDeserializer<Time?>() {
        override fun deserialize(jsonParser: JsonParser, deserializationContext: DeserializationContext?): Time {
            val value: JsonNode = ObjectUtil.defaultMapper.readTree(jsonParser)
            return if (value.has("type") && value["type"].asText() == "interval") {
                Interval(ObjectUtil.defaultMapper.treeToValue(value["from"], GrainedTime::class.java),
                         ObjectUtil.defaultMapper.treeToValue(value["to"], GrainedTime::class.java))
            } else {
                GrainedTime(DateTime.parse(value["value"].asText()), value["grain"].asText())
            }
        }
    }

}