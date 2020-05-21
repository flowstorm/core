package com.promethist.core.type

import java.math.BigDecimal
import java.time.ZonedDateTime

typealias DateTime = ZonedDateTime
typealias PropertyMap = Map<String, Any>
typealias MutablePropertyMap = MutableMap<String, Any>

interface ValueCollection

class BooleanMutableList(vararg values: Boolean) : ArrayList<Boolean>(values.asList()), ValueCollection
class StringMutableList(vararg values: String) : ArrayList<String>(values.asList()), ValueCollection
class IntMutableList(vararg values: Int) : ArrayList<Int>(values.asList()), ValueCollection
class LongMutableList(vararg values: Long) : ArrayList<Long>(values.asList()), ValueCollection
class FloatMutableList(vararg values: Float) : ArrayList<Float>(values.asList()), ValueCollection
class DoubleMutableList(vararg values: Double) : ArrayList<Double>(values.asList()), ValueCollection
class BigDecimalMutableList(vararg values: BigDecimal) : ArrayList<BigDecimal>(values.asList()), ValueCollection
class DateTimeMutableList(vararg values: DateTime) : ArrayList<DateTime>(values.asList()), ValueCollection

class BooleanValueMutableList(vararg values: Value<Boolean>) : ArrayList<Value<Boolean>>(values.asList()), ValueCollection
class StringValueMutableList(vararg values: Value<String>) : ArrayList<Value<String>>(values.asList()), ValueCollection
class IntValueMutableList(vararg values: Value<Int>) : ArrayList<Value<Int>>(values.asList()), ValueCollection
class LongValueMutableList(vararg values: Value<Long>) : ArrayList<Value<Long>>(values.asList()), ValueCollection
class FloatValueMutableList(vararg values: Value<Float>) : ArrayList<Value<Float>>(values.asList()), ValueCollection
class DoubleValueMutableList(vararg values: Value<Double>) : ArrayList<Value<Double>>(values.asList()), ValueCollection
class BigDecimalValueMutableList(vararg values: Value<BigDecimal>) : ArrayList<Value<BigDecimal>>(values.asList()), ValueCollection
class DateTimeValueMutableList(vararg values: Value<DateTime>) : ArrayList<Value<DateTime>>(values.asList()), ValueCollection

class BooleanMutableSet(vararg values: Boolean) : HashSet<Boolean>(values.asList()), ValueCollection
class StringMutableSet(vararg values: String) : HashSet<String>(values.asList()), ValueCollection
class IntMutableSet(vararg values: Int) : HashSet<Int>(values.asList()), ValueCollection
class LongMutableSet(vararg values: Long) : HashSet<Long>(values.asList()), ValueCollection
class FloatMutableSet(vararg values: Float) : HashSet<Float>(values.asList()), ValueCollection
class DoubleMutableSet(vararg values: Double) : HashSet<Double>(values.asList()), ValueCollection
class BigDecimalMutableSet(vararg values: BigDecimal) : HashSet<BigDecimal>(values.asList()), ValueCollection
class DateTimeMutableSet(vararg values: DateTime) : HashSet<DateTime>(values.asList()), ValueCollection

class BooleanValueMutableSet(vararg values: Value<Boolean>) : ValueMutableSet<Boolean>(values.asList()), ValueCollection
class StringValueMutableSet(vararg values: Value<String>) : ValueMutableSet<String>(values.asList()), ValueCollection
class IntValueMutableSet(vararg values: Value<Int>) : ValueMutableSet<Int>(values.asList()), ValueCollection
class LongValueMutableSet(vararg values: Value<Long>) : ValueMutableSet<Long>(values.asList()), ValueCollection
class FloatValueMutableSet(vararg values: Value<Float>) : ValueMutableSet<Float>(values.asList()), ValueCollection
class DoubleValueMutableSet(vararg values: Value<Double>) : ValueMutableSet<Double>(values.asList()), ValueCollection
class BigDecimalValueMutableSet(vararg values: Value<BigDecimal>) : ValueMutableSet<BigDecimal>(values.asList()), ValueCollection
class DateTimeValueMutableSet(vararg values: Value<DateTime>) : ValueMutableSet<DateTime>(values.asList()), ValueCollection
