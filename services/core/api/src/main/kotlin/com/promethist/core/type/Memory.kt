package com.promethist.core.type

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.promethist.core.Defaults
import java.math.BigDecimal

open class Memory<V: Any>(
        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "_type", include = JsonTypeInfo.As.EXTERNAL_PROPERTY)
        @JsonSubTypes(value = [

            JsonSubTypes.Type(value = Boolean::class, name = "Boolean"),
            JsonSubTypes.Type(value = String::class, name = "String"),
            JsonSubTypes.Type(value = Int::class, name = "Int"),
            JsonSubTypes.Type(value = Long::class, name = "Long"),
            JsonSubTypes.Type(value = Float::class, name = "Float"),
            JsonSubTypes.Type(value = Double::class, name = "Double"),
            JsonSubTypes.Type(value = BigDecimal::class, name = "BigDecimal"),
            JsonSubTypes.Type(value = DateTime::class, name = "ZonedDateTime"),

            JsonSubTypes.Type(value = BooleanMutableSet::class, name = "BooleanMutableSet"),
            JsonSubTypes.Type(value = StringMutableSet::class, name = "StringMutableSet"),
            JsonSubTypes.Type(value = IntMutableSet::class, name = "IntMutableSet"),
            JsonSubTypes.Type(value = LongMutableSet::class, name = "LongMutableSet"),
            JsonSubTypes.Type(value = FloatMutableSet::class, name = "FloatMutableSet"),
            JsonSubTypes.Type(value = DoubleMutableSet::class, name = "DoubleMutableSet"),
            JsonSubTypes.Type(value = BigDecimalMutableSet::class, name = "BigDecimalMutableSet"),
            JsonSubTypes.Type(value = DateTimeMutableSet::class, name = "DateTimeMutableSet"),

            JsonSubTypes.Type(value = BooleanMutableList::class, name = "BooleanMutableList"),
            JsonSubTypes.Type(value = StringMutableList::class, name = "StringMutableList"),
            JsonSubTypes.Type(value = IntMutableList::class, name = "IntMutableList"),
            JsonSubTypes.Type(value = LongMutableList::class, name = "LongMutableList"),
            JsonSubTypes.Type(value = FloatMutableList::class, name = "FloatMutableList"),
            JsonSubTypes.Type(value = DoubleMutableList::class, name = "DoubleMutableList"),
            JsonSubTypes.Type(value = BigDecimalMutableList::class, name = "BigDecimalMutableList"),
            JsonSubTypes.Type(value = DateTimeMutableList::class, name = "DateTimeMutableList")
        ])
        var _value: V,
        var _type: String = _value::class.simpleName!!
) : PersistentObject {
    @get:JsonIgnore
    var value: V
        get() = _value
        set(value) {
            touch()
            _value = value
        }
    @get:JsonIgnore val booleanValue get() = value as Boolean
    @get:JsonIgnore val stringValue get() = value.toString()
    @get:JsonIgnore val intValue get() = value as Int
    @get:JsonIgnore val longValue get() = value as Long
    @get:JsonIgnore val floatValue get() = value as Float
    @get:JsonIgnore val doubleValue get() = value as Double
    @get:JsonIgnore val bigDecimalValue get() = value as BigDecimal
    @get:JsonIgnore val dateTimeValue get() = value as DateTime
    var time = DateTime.now()
    var count = 0

    override fun equals(other: Any?): Boolean = if (other is Memory<*>) (_value == other._value) else false

    override fun hashCode(): Int = _value.hashCode()

    override fun toString(): String = "${this::class.simpleName}(value=$_value, count=$count, time=$time)"

    fun touch() {
        count++
        time = DateTime.now()
    }

    companion object {

        val ZERO_TIME = DateTime.of(0, 1, 1, 0, 0, 0, 0, Defaults.zoneId)

        fun pack(any: Any): PersistentObject =
            if (any is PersistentObject) {
                any
            } else when (any) {
                is Boolean, is String, is Int, is Long, is Float, is Double, is BigDecimal, is DateTime, is ValueCollection -> Memory(any)
                else -> error("unsupported value type ${any::class.qualifiedName}")
            }
    }
}