package com.promethist.core.type

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.promethist.core.Defaults
import java.math.BigDecimal
import kotlin.reflect.KClass

open class Value<V: Any> internal constructor(//v: V) {
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXTERNAL_PROPERTY)
    @JsonSubTypes(value = [
        // primitives
        JsonSubTypes.Type(value = Boolean::class, name = "Boolean"),
        JsonSubTypes.Type(value = String::class, name = "String"),
        JsonSubTypes.Type(value = Int::class, name = "Int"),
        JsonSubTypes.Type(value = Long::class, name = "Long"),
        JsonSubTypes.Type(value = Float::class, name = "Float"),
        JsonSubTypes.Type(value = Double::class, name = "Double"),
        JsonSubTypes.Type(value = BigDecimal::class, name = "BigDecimal"),
        JsonSubTypes.Type(value = DateTime::class, name = "ZonedDateTime"),

        // primitive lists
        JsonSubTypes.Type(value = BooleanMutableList::class, name = "BooleanMutableList"),
        JsonSubTypes.Type(value = StringMutableList::class, name = "StringMutableList"),
        JsonSubTypes.Type(value = IntMutableList::class, name = "IntMutableList"),
        JsonSubTypes.Type(value = LongMutableList::class, name = "LongMutableList"),
        JsonSubTypes.Type(value = FloatMutableList::class, name = "FloatMutableList"),
        JsonSubTypes.Type(value = DoubleMutableList::class, name = "DoubleMutableList"),
        JsonSubTypes.Type(value = BigDecimalMutableList::class, name = "BigDecimalMutableList"),
        JsonSubTypes.Type(value = DateTimeMutableList::class, name = "DateTimeMutableList"),

        // object value lists
        JsonSubTypes.Type(value = BooleanValueMutableList::class, name = "BooleanValueMutableList"),
        JsonSubTypes.Type(value = StringValueMutableList::class, name = "StringValueMutableList"),
        JsonSubTypes.Type(value = IntValueMutableList::class, name = "IntValueMutableList"),
        JsonSubTypes.Type(value = LongValueMutableList::class, name = "LongValueMutableList"),
        JsonSubTypes.Type(value = FloatValueMutableList::class, name = "FloatValueMutableList"),
        JsonSubTypes.Type(value = DoubleValueMutableList::class, name = "DoubleValueMutableList"),
        JsonSubTypes.Type(value = BigDecimalValueMutableList::class, name = "BigDecimalValueMutableList"),
        JsonSubTypes.Type(value = DateTimeValueMutableList::class, name = "DateTimeValueMutableList"),

        // primitive sets
        JsonSubTypes.Type(value = BooleanMutableSet::class, name = "BooleanMutableSet"),
        JsonSubTypes.Type(value = StringMutableSet::class, name = "StringMutableSet"),
        JsonSubTypes.Type(value = IntMutableSet::class, name = "IntMutableSet"),
        JsonSubTypes.Type(value = LongMutableSet::class, name = "LongMutableSet"),
        JsonSubTypes.Type(value = FloatMutableSet::class, name = "FloatMutableSet"),
        JsonSubTypes.Type(value = DoubleMutableSet::class, name = "DoubleMutableSet"),
        JsonSubTypes.Type(value = BigDecimalMutableSet::class, name = "BigDecimalMutableSet"),
        JsonSubTypes.Type(value = DateTimeMutableSet::class, name = "DateTimeMutableSet"),

        // object value sets
        JsonSubTypes.Type(value = BooleanValueMutableSet::class, name = "BooleanValueMutableSet"),
        JsonSubTypes.Type(value = StringValueMutableSet::class, name = "StringValueMutableSet"),
        JsonSubTypes.Type(value = IntValueMutableSet::class, name = "IntValueMutableSet"),
        JsonSubTypes.Type(value = LongValueMutableSet::class, name = "LongValueMutableSet"),
        JsonSubTypes.Type(value = FloatValueMutableSet::class, name = "FloatValueMutableSet"),
        JsonSubTypes.Type(value = DoubleValueMutableSet::class, name = "DoubleValueMutableSet"),
        JsonSubTypes.Type(value = BigDecimalValueMutableSet::class, name = "BigDecimalValueMutableSet"),
        JsonSubTypes.Type(value = DateTimeValueMutableSet::class, name = "DateTimeValueMutableSet")
    ]) var value: V) {/*

    var value: V = v
        get() {
            atime = DateTime.now()
            acount++
            return field
        }
        set (v) {
            time = DateTime.now()
            count++
            field = v
        }*/
    //val type: String = v::class.simpleName!!
    var time = DateTime.now()
    var count = 0

    override fun equals(other: Any?): Boolean = if (other is Value<*>) (value == other.value) else false

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = "${this::class.simpleName}(value=$value, count=$count, time=$time)"

    companion object {

        val ZERO_TIME = DateTime.of(0, 1, 1, 0, 0, 0, 0, Defaults.zoneId)

        inline fun <reified T : Any> create(): T = create(T::class) as T

        fun create(clazz: KClass<*>): Any = when (clazz) {

            Boolean::class -> false
            String::class -> ""
            Int::class -> 0
            Long::class -> 0L
            Float::class -> 0.0F
            Double::class -> 0.0
            BigDecimal::class -> BigDecimal(0)
            DateTime::class -> ZERO_TIME

            BooleanValue::class -> BooleanValue()
            StringValue::class -> StringValue()
            IntValue::class -> IntValue()
            LongValue::class -> LongValue()
            FloatValue::class -> FloatValue()
            DoubleValue::class -> DoubleValue()
            BigDecimalValue::class -> BigDecimalValue()
            DateTimeValue::class -> DateTimeValue()

            BooleanMutableList::class -> BooleanMutableList()
            StringMutableList::class -> StringMutableList()
            IntMutableList::class -> IntMutableList()
            LongMutableList::class -> LongMutableList()
            FloatMutableList::class -> FloatMutableList()
            DoubleMutableList::class -> DoubleMutableList()
            BigDecimalMutableList::class -> BigDecimalMutableList()
            DateTimeMutableList::class -> DateTimeMutableList()

            BooleanValueMutableList::class -> BooleanValueMutableList()
            StringValueMutableList::class -> StringValueMutableList()
            IntValueMutableList::class -> IntValueMutableList()
            LongValueMutableList::class -> LongValueMutableList()
            FloatValueMutableList::class -> FloatValueMutableList()
            DoubleValueMutableList::class -> DoubleValueMutableList()
            BigDecimalValueMutableList::class -> BigDecimalValueMutableList()
            DateTimeValueMutableList::class -> DateTimeValueMutableList()

            BooleanMutableSet::class -> BooleanMutableSet()
            StringMutableSet::class -> StringMutableSet()
            IntMutableSet::class -> IntMutableSet()
            LongMutableSet::class -> LongMutableSet()
            FloatMutableSet::class -> FloatMutableSet()
            DoubleMutableSet::class -> DoubleMutableSet()
            BigDecimalMutableSet::class -> BigDecimalMutableSet()
            DateTimeMutableSet::class -> DateTimeMutableSet()

            BooleanValueMutableSet::class -> BooleanValueMutableSet()
            StringValueMutableSet::class -> StringValueMutableSet()
            IntValueMutableSet::class -> IntValueMutableSet()
            LongValueMutableSet::class -> LongValueMutableSet()
            FloatValueMutableSet::class -> FloatValueMutableSet()
            DoubleValueMutableSet::class -> DoubleValueMutableSet()
            BigDecimalValueMutableSet::class -> BigDecimalValueMutableSet()
            DateTimeValueMutableSet::class -> DateTimeValueMutableSet()

            else -> error("unsupported value type ${clazz.qualifiedName}")
        }

        fun pack(any: Any): Value<*> =
            if (any is Value<*>) {
                any
            } else when (any) {
                is Boolean, is String, is Int, is Long, is Float, is Double, is BigDecimal, is DateTime, is ValueCollection -> Value(any)
                else -> error("unsupported value type ${any::class.qualifiedName}")
            }.apply {
                time = DateTime.now()
                count++
            }
    }
}