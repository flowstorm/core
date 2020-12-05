package org.promethist.core.type

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.promethist.core.type.value.*
import org.promethist.core.type.value.Number
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.javaField

class DucklingEntity: InputEntity("Text", "", 1.0F, "duckling") {
    val start: Int = 0
    val end: Int = 0
    val latent: Boolean = false
    var dim: String = ""
        set(value) {
            field = value
            className = this::class.declaredMemberProperties.first {
                it.name == "value"
            }.javaField!!.getAnnotation(JsonSubTypes::class.java).value.find {
                it.name == value
            }?.value?.simpleName ?: error("cannot find value class for duckling dim \"$value\"")
            if (className == "Time") {
                className = this.value::class.simpleName!!
            }
        }

    @JsonProperty("body")
    override var text = ""

    @JsonProperty("value")
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "dim", include = JsonTypeInfo.As.EXTERNAL_PROPERTY, visible = true)
    @JsonSubTypes(value = [
        JsonSubTypes.Type(value = Time::class, name = "time"),
        JsonSubTypes.Type(value = Quantity::class, name = "quantity"),
        JsonSubTypes.Type(value = Currency::class, name = "amount-of-money"),
        JsonSubTypes.Type(value = Distance::class, name = "distance"),
        JsonSubTypes.Type(value = Temperature::class, name = "temperature"),
        JsonSubTypes.Type(value = Volume::class, name = "volume"),
        JsonSubTypes.Type(value = CreditCard::class, name = "credit-card-number"),
        JsonSubTypes.Type(value = Duration::class, name = "duration"),
        JsonSubTypes.Type(value = Number::class, name = "number"),
        JsonSubTypes.Type(value = Ordinal::class, name = "ordinal"),
        JsonSubTypes.Type(value = Email::class, name = "email"),
        JsonSubTypes.Type(value = Phone::class, name = "phone-number"),
        JsonSubTypes.Type(value = URL::class, name = "url")
    ])
    override val value: Value = Text(text)
}