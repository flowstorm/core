package com.promethist.core.type

import java.math.BigDecimal

typealias PropertyMap = Map<String, Any>
typealias MutablePropertyMap = MutableMap<String, Any>

interface ValueCollection

open class ValueList<V : Any>(values: Collection<V>) : ArrayList<V>(values), ValueCollection {
    fun remember(value: V) = add(value)
}
open class ValueSet<V : Any>(values: Collection<V>) : HashSet<V>(values), ValueCollection {
    fun remember(value: V) = add(value)
}

class BooleanMutableList(vararg values: Boolean) : ValueList<Boolean>(values.asList())
class StringMutableList(vararg values: String) : ValueList<String>(values.asList())
class IntMutableList(vararg values: Int) : ValueList<Int>(values.asList())
class LongMutableList(vararg values: Long) : ValueList<Long>(values.asList())
class FloatMutableList(vararg values: Float) : ValueList<Float>(values.asList())
class DoubleMutableList(vararg values: Double) : ValueList<Double>(values.asList())
class BigDecimalMutableList(vararg values: BigDecimal) : ValueList<BigDecimal>(values.asList())
class DateTimeMutableList(vararg values: DateTime) : ValueList<DateTime>(values.asList())

class BooleanMutableSet(vararg values: Boolean) : ValueSet<Boolean>(values.asList())
class StringMutableSet(vararg values: String) : ValueSet<String>(values.asList())
class IntMutableSet(vararg values: Int) : ValueSet<Int>(values.asList())
class LongMutableSet(vararg values: Long) : ValueSet<Long>(values.asList())
class FloatMutableSet(vararg values: Float) : ValueSet<Float>(values.asList())
class DoubleMutableSet(vararg values: Double) : ValueSet<Double>(values.asList())
class BigDecimalMutableSet(vararg values: BigDecimal) : ValueSet<BigDecimal>(values.asList())
class DateTimeMutableSet(vararg values: DateTime) : ValueSet<DateTime>(values.asList())

interface MemoryCollection<V : Any> : Collection<Memory<V>>, PersistentObject

class MemoryMutableList<V : Any>(vararg memories: Memory<V>) : ArrayList<Memory<V>>(memories.asList()), MemoryCollection<V> {
    fun remember(value: V) = add(Memory(value))
}

class MemoryMutableSet<V : Any>(vararg memories: Memory<V>) : HashSet<Memory<V>>(memories.asList()), MemoryCollection<V> {
    override fun add(memory: Memory<V>): Boolean {
        for (m in this) {
            if (m == memory) {
                m.touch()
                return true
            }
        }
        return super.add(memory)
    }
    fun remember(value: V) = add(Memory(value))
}
