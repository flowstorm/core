package com.promethist.core.dialogue

import com.promethist.core.type.DateTime
import com.promethist.core.type.Memory
import com.promethist.core.type.MemoryMutableSet
import kotlin.random.Random

abstract class SequenceAttribute<E, V : Any>(
        val list: List<E>,
        private val memories: MemoryMutableSet<V>,
        val nextValue: (SequenceAttribute<E, V>.() -> E?)) {

    val next get() = nextValue(this)
    val last get() = memories.maxBy { it.time }?.let { memory ->
        list.find { toMemoryValue(it) == memory.value}
    } ?: error("no item in sequence (access next property first)")

    fun nextRandom(minDuration: DateTimeUnit = 1.day, maxCount: Int = Int.MAX_VALUE, resetDuration: DateTimeUnit? = null): E? {
        val values = mutableListOf<V>()
        val now = DateTime.now()
        if (resetDuration != null)
            memories.filter { it.time + resetDuration <= now }.forEach { it.count = 0 }
        list.forEach { e ->
            val value = toMemoryValue(e)
            val lastMemory = memories.find {
                (it.value == value) && (it.count >= maxCount - 1 || it.time + minDuration > now)
            }
            if (lastMemory == null)
                values.add(value)
        }
        if (values.isNotEmpty()) {
            val value = values[Random.nextInt(values.size)]
            memories.add(Memory(value))
            return list.find { toMemoryValue(it) == value }
        }
        return null
    }

    fun nextInLine(minDuration: DateTimeUnit = 1.day, maxCount: Int = Int.MAX_VALUE, resetDuration: DateTimeUnit? = null): E? {
        TODO("SequenceAttribute.nextInLine method not implemented yet")
    }

    abstract fun toMemoryValue(v: E): V
}