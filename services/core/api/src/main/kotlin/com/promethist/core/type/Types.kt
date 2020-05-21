package com.promethist.core.type

import java.math.BigDecimal
import java.time.ZonedDateTime

typealias DateTime = ZonedDateTime
typealias PropertyMap = Map<String, Any>
typealias MutablePropertyMap = MutableMap<String, Any>

class BooleanValue(value: Boolean = create()) : Value<Boolean>(value)
class StringValue(value: String = create()) : Value<String>(value)
class IntValue(value: Int = create()) : Value<Int>(value)
class LongValue(value: Long = create()) : Value<Long>(value)
class FloatValue(value: Float = create()) : Value<Float>(value)
class DoubleValue(value: Double = create()) : Value<Double>(value)
class BigDecimalValue(value: BigDecimal = create()) : Value<BigDecimal>(value)
class DateTimeValue(value: DateTime = create()) : Value<DateTime>(value)

interface ValueCollection

class BooleanMutableList : ArrayList<Boolean>(), ValueCollection
class StringMutableList : ArrayList<String>(), ValueCollection
class IntMutableList : ArrayList<Int>(), ValueCollection
class LongMutableList : ArrayList<Long>(), ValueCollection
class FloatMutableList : ArrayList<Float>(), ValueCollection
class DoubleMutableList : ArrayList<Double>(), ValueCollection
class BigDecimalMutableList : ArrayList<BigDecimal>(), ValueCollection
class DateTimeMutableList : ArrayList<DateTime>(), ValueCollection

class BooleanValueMutableList : ArrayList<Value<Boolean>>(), ValueCollection
class StringValueMutableList : ArrayList<Value<String>>(), ValueCollection
class IntValueMutableList : ArrayList<Value<Int>>(), ValueCollection
class LongValueMutableList : ArrayList<Value<Long>>(), ValueCollection
class FloatValueMutableList : ArrayList<Value<Float>>(), ValueCollection
class DoubleValueMutableList : ArrayList<Value<Double>>(), ValueCollection
class BigDecimalValueMutableList : ArrayList<Value<BigDecimal>>(), ValueCollection
class DateTimeValueMutableList : ArrayList<Value<DateTime>>(), ValueCollection

class BooleanMutableSet : LinkedHashSet<Boolean>(), ValueCollection
class StringMutableSet : LinkedHashSet<String>(), ValueCollection
class IntMutableSet : LinkedHashSet<Int>(), ValueCollection
class LongMutableSet : LinkedHashSet<Long>(), ValueCollection
class FloatMutableSet : LinkedHashSet<Float>(), ValueCollection
class DoubleMutableSet : LinkedHashSet<Double>(), ValueCollection
class BigDecimalMutableSet : LinkedHashSet<BigDecimal>(), ValueCollection
class DateTimeMutableSet : LinkedHashSet<DateTime>(), ValueCollection

class BooleanValueMutableSet : ValueMutableSet<Boolean>(), ValueCollection
class StringValueMutableSet : ValueMutableSet<String>(), ValueCollection
class IntValueMutableSet : ValueMutableSet<Int>(), ValueCollection
class LongValueMutableSet : ValueMutableSet<Long>(), ValueCollection
class FloatValueMutableSet : ValueMutableSet<Float>(), ValueCollection
class DoubleValueMutableSet : ValueMutableSet<Double>(), ValueCollection
class BigDecimalValueMutableSet : ValueMutableSet<BigDecimal>(), ValueCollection
class DateTimeValueMutableSet : ValueMutableSet<DateTime>(), ValueCollection
