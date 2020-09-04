package com.promethist.core.type

typealias PropertyMap = Map<String, Any>
typealias MutablePropertyMap = MutableMap<String, Any>

interface ValueCollection

open class ValueList<V : Any>(values: Collection<V>) : ArrayList<V>(values), ValueCollection
open class ValueSet<V : Any>(values: Collection<V>) : HashSet<V>(values), ValueCollection

class BooleanMutableList(values: Collection<Boolean>) : ValueList<Boolean>(values) {
    constructor() : this(emptyList())
    constructor(vararg values: Boolean) : this(values.asList())
}

class StringMutableList(values: Collection<String>) : ValueList<String>(values) {
    constructor() : this(emptyList())
    constructor(vararg values: String) : this(values.asList())
}

class IntMutableList(values: Collection<Int>) : ValueList<Int>(values) {
    constructor() : this(emptyList())
    constructor(vararg values: Int) : this(values.asList())
}

class LongMutableList(values: Collection<Long>) : ValueList<Long>(values) {
    constructor() : this(emptyList())
    constructor(vararg values: Long) : this(values.asList())
}

class FloatMutableList(values: Collection<Float>) : ValueList<Float>(values) {
    constructor() : this(emptyList())
    constructor(vararg values: Float) : this(values.asList())
}

class DoubleMutableList(values: Collection<Double>) : ValueList<Double>(values) {
    constructor() : this(emptyList())
    constructor(vararg values: Double) : this(values.asList())
}

class BigDecimalMutableList(values: Collection<Decimal>) : ValueList<Decimal>(values) {
    constructor() : this(emptyList())
    constructor(vararg values: Decimal) : this(values.asList())
}

class DateTimeMutableList(values: Collection<DateTime>) : ValueList<DateTime>(values) {
    constructor() : this(emptyList())
    constructor(vararg values: DateTime) : this(values.asList())
}

class LocationMutableList(values: Collection<Location>) : ValueList<Location>(values) {
    constructor() : this(emptyList())
    constructor(vararg values: Location) : this(values.asList())
}

class DynamicMutableList(values: Collection<Dynamic>) : ValueList<Dynamic>(values) {
    constructor() : this(emptyList())
    constructor(vararg values: Dynamic) : this(values.asList())
}

class BooleanMutableSet(values: Collection<Boolean>) : ValueSet<Boolean>(values) {
    constructor() : this(emptyList())
    constructor(vararg values: Boolean) : this(values.asList())
}

class StringMutableSet(values: Collection<String>) : ValueSet<String>(values) {
    constructor() : this(emptySet())
    constructor(vararg values: String) : this(values.asList())
}

class IntMutableSet(values: Collection<Int>) : ValueSet<Int>(values) {
    constructor() : this(emptySet())
    constructor(vararg values: Int) : this(values.asList())
}

class LongMutableSet(values: Collection<Long>) : ValueSet<Long>(values) {
    constructor() : this(emptySet())
    constructor(vararg values: Long) : this(values.asList())
}

class FloatMutableSet(values: Collection<Float>) : ValueSet<Float>(values) {
    constructor() : this(emptySet())
    constructor(vararg values: Float) : this(values.asList())
}

class DoubleMutableSet(values: Collection<Double>) : ValueSet<Double>(values) {
    constructor() : this(emptySet())
    constructor(vararg values: Double) : this(values.asList())
}

class BigDecimalMutableSet(values: Collection<Decimal>) : ValueSet<Decimal>(values) {
    constructor() : this(emptySet())
    constructor(vararg values: Decimal) : this(values.asList())
}

class DateTimeMutableSet(values: Collection<DateTime>) : ValueSet<DateTime>(values) {
    constructor() : this(emptySet())
    constructor(vararg values: DateTime) : this(values.asList())
}

class LocationMutableSet(values: Collection<Location>) : ValueSet<Location>(values) {
    constructor() : this(emptySet())
    constructor(vararg values: Location) : this(values.asList())
}

class DynamicMutableSet(values: Collection<Dynamic>) : ValueSet<Dynamic>(values) {
    constructor() : this(emptySet())
    constructor(vararg values: Dynamic) : this(values.asList())
}

interface MemoryCollection<V : Any> : MutableCollection<Memory<V>>, Memorable

class MemoryMutableList<V : Any>(memories: Collection<Memory<V>>) : ArrayList<Memory<V>>(memories), MemoryCollection<V> {
    constructor() : this(emptyList())
    constructor(vararg memories: Memory<V>) : this(memories.asList())
    val values get() = map { it.value }
}

class MemoryMutableSet<V : Any>(memories: Collection<Memory<V>>) : HashSet<Memory<V>>(memories), MemoryCollection<V> {

    constructor() : this(emptySet())
    constructor(vararg memories: Memory<V>) : this(memories.asList())
    val values get() = map { it.value }
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
fun MemoryCollection<Decimal>.add(value: Decimal) = add(Memory(value))
fun MemoryCollection<DateTime>.add(value: DateTime) = add(Memory(value))
fun MemoryCollection<Location>.add(value: Location) = add(Memory(value))
