package com.promethist.core.type

import java.math.BigDecimal

typealias PropertyMap = Map<String, Any>
typealias MutablePropertyMap = MutableMap<String, Any>

interface ValueCollection

open class ValueList<V : Any>(values: Collection<V>) : ArrayList<V>(values), ValueCollection
open class ValueSet<V : Any>(values: Collection<V>) : HashSet<V>(values), ValueCollection

class BooleanMutableList(vararg values: Boolean) : ValueList<Boolean>(values.asList())
class StringMutableList(vararg values: String) : ValueList<String>(values.asList())
class IntMutableList(vararg values: Int) : ValueList<Int>(values.asList())
class LongMutableList(vararg values: Long) : ValueList<Long>(values.asList())
class FloatMutableList(vararg values: Float) : ValueList<Float>(values.asList())
class DoubleMutableList(vararg values: Double) : ValueList<Double>(values.asList())
class BigDecimalMutableList(vararg values: BigDecimal) : ValueList<BigDecimal>(values.asList())
class DateTimeMutableList(vararg values: DateTime) : ValueList<DateTime>(values.asList())
class LocationMutableList(vararg values: Location) : ValueList<Location>(values.asList())

class BooleanMutableSet(vararg values: Boolean) : ValueSet<Boolean>(values.asList())
class StringMutableSet(vararg values: String) : ValueSet<String>(values.asList())
class IntMutableSet(vararg values: Int) : ValueSet<Int>(values.asList())
class LongMutableSet(vararg values: Long) : ValueSet<Long>(values.asList())
class FloatMutableSet(vararg values: Float) : ValueSet<Float>(values.asList())
class DoubleMutableSet(vararg values: Double) : ValueSet<Double>(values.asList())
class BigDecimalMutableSet(vararg values: BigDecimal) : ValueSet<BigDecimal>(values.asList())
class DateTimeMutableSet(vararg values: DateTime) : ValueSet<DateTime>(values.asList())
class LocationMutableSet(vararg values: Location) : ValueSet<Location>(values.asList())

interface MemoryCollection<V : Any> : MutableCollection<Memory<V>>, PersistentObject

class MemoryMutableList<V : Any>(vararg memories: Memory<V>) : ArrayList<Memory<V>>(memories.asList()), MemoryCollection<V>

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
}

fun MemoryCollection<Boolean>.add(value: Boolean) = add(Memory(value))
fun MemoryCollection<String>.add(value: String) = add(Memory(value))
fun MemoryCollection<Int>.add(value: Int) = add(Memory(value))
fun MemoryCollection<Long>.add(value: Long) = add(Memory(value))
fun MemoryCollection<Float>.add(value: Float) = add(Memory(value))
fun MemoryCollection<Double>.add(value: Double) = add(Memory(value))
fun MemoryCollection<BigDecimal>.add(value: BigDecimal) = add(Memory(value))
fun MemoryCollection<DateTime>.add(value: DateTime) = add(Memory(value))
fun MemoryCollection<Location>.add(value: Location) = add(Memory(value))
