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

class BooleanMutableSet(vararg values: Boolean) : HashSet<Boolean>(values.asList()), ValueCollection
class StringMutableSet(vararg values: String) : HashSet<String>(values.asList()), ValueCollection
class IntMutableSet(vararg values: Int) : HashSet<Int>(values.asList()), ValueCollection
class LongMutableSet(vararg values: Long) : HashSet<Long>(values.asList()), ValueCollection
class FloatMutableSet(vararg values: Float) : HashSet<Float>(values.asList()), ValueCollection
class DoubleMutableSet(vararg values: Double) : HashSet<Double>(values.asList()), ValueCollection
class BigDecimalMutableSet(vararg values: BigDecimal) : HashSet<BigDecimal>(values.asList()), ValueCollection
class DateTimeMutableSet(vararg values: DateTime) : HashSet<DateTime>(values.asList()), ValueCollection

interface MemoryCollection<V : Any> : Collection<Memory<V>>, PersistentObject

class MemoryMutableList<V : Any>(vararg values: Memory<V>) : ArrayList<Memory<V>>(values.asList()), MemoryCollection<V>
class MemoryMutableSet<V : Any>(vararg values: Memory<V>) : HashSet<Memory<V>>(values.asList()), MemoryCollection<V> {
    override fun add(e: Memory<V>): Boolean {
        for (m in this) {
            if (m == e) {
                m.touch()
                return true
            }
        }
        return super.add(e)
    }
}
