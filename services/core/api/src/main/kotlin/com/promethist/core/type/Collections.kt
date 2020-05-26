package com.promethist.core.type

import java.math.BigDecimal

typealias PropertyMap = Map<String, Any>
typealias MutablePropertyMap = MutableMap<String, Any>

interface ValueCollection

open class ValueList<V : Any>(values: Collection<V>) : ArrayList<V>(values), ValueCollection
open class ValueSet<V : Any>(values: Collection<V>) : HashSet<V>(values), ValueCollection

class BooleanMutableList(values: Collection<Boolean>) : ValueList<Boolean>(values) {
    constructor(vararg values: Boolean) : this(values.asList())
}

class StringMutableList(values: Collection<String>) : ValueList<String>(values) {
    constructor(vararg values: String) : this(values.asList())
}

class IntMutableList(values: Collection<Int>) : ValueList<Int>(values) {
    constructor(vararg values: Int) : this(values.asList())
}

class LongMutableList(values: Collection<Long>) : ValueList<Long>(values) {
    constructor(vararg values: Long) : this(values.asList())
}

class FloatMutableList(values: Collection<Float>) : ValueList<Float>(values) {
    constructor(vararg values: Float) : this(values.asList())
}

class DoubleMutableList(values: Collection<Double>) : ValueList<Double>(values) {
    constructor(vararg values: Double) : this(values.asList())
}

class BigDecimalMutableList(values: Collection<BigDecimal>) : ValueList<BigDecimal>(values) {
    constructor(vararg values: BigDecimal) : this(values.asList())
}

class DateTimeMutableList(values: Collection<DateTime>) : ValueList<DateTime>(values) {
    constructor(vararg values: DateTime) : this(values.asList())
}

class LocationMutableList(values: Collection<Location>) : ValueList<Location>(values) {
    constructor(vararg values: Location) : this(values.asList())
}

class BooleanMutableSet(values: Collection<Boolean>) : ValueSet<Boolean>(values) {
    constructor(vararg values: Boolean) : this(values.asList())
}

class StringMutableSet(values: Collection<String>) : ValueSet<String>(values) {
    constructor(vararg values: String) : this(values.asList())
}

class IntMutableSet(values: Collection<Int>) : ValueSet<Int>(values) {
    constructor(vararg values: Int) : this(values.asList())
}

class LongMutableSet(values: Collection<Long>) : ValueSet<Long>(values) {
    constructor(vararg values: Long) : this(values.asList())
}

class FloatMutableSet(values: Collection<Float>) : ValueSet<Float>(values) {
    constructor(vararg values: Float) : this(values.asList())
}

class DoubleMutableSet(values: Collection<Double>) : ValueSet<Double>(values) {
    constructor(vararg values: Double) : this(values.asList())
}

class BigDecimalMutableSet(values: Collection<BigDecimal>) : ValueSet<BigDecimal>(values) {
    constructor(vararg values: BigDecimal) : this(values.asList())
}

class DateTimeMutableSet(values: Collection<DateTime>) : ValueSet<DateTime>(values) {
    constructor(vararg values: DateTime) : this(values.asList())
}

class LocationMutableSet(values: Collection<Location>) : ValueSet<Location>(values) {
    constructor(vararg values: Location) : this(values.asList())
}

interface MemoryCollection<V : Any> : MutableCollection<Memory<V>>, Memorable

class MemoryMutableList<V : Any>(memories: Collection<Memory<V>>) : ArrayList<Memory<V>>(memories), MemoryCollection<V> {
    constructor(vararg memories: Memory<V>) : this(memories.asList())
}

class MemoryMutableSet<V : Any>(memories: Collection<Memory<V>>) : HashSet<Memory<V>>(memories), MemoryCollection<V> {

    constructor(vararg memories: Memory<V>) : this(memories.asList())

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
