package com.promethist.core.type

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver
import com.fasterxml.jackson.databind.util.StdConverter
import com.promethist.core.dialogue.AbstractDialogue

@JsonSerialize(converter = Memory.MemoryConverter::class)
open class Memory<V : Any>(
        @JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "_type", include = JsonTypeInfo.As.EXTERNAL_PROPERTY)
        @JsonTypeIdResolver(MemoryTypeIdResolver::class)
        var _value: V,
        var _type: String = MemoryTypeIdResolver().idFromValue(_value),
        var _origType: String = _type,
        var serializable: Boolean = false
) : Memorable {

    companion object {
        fun canContainNatively(it: Any) = it is Boolean || it is String || it is Int || it is Long || it is Float
                || it is Double || it is Decimal || it is DateTime || it is Dynamic || it is Location || it is ValueCollection

        fun canContain(it: Any) = canContainNatively(it) || it is Collection<*> || it is Map<*, *>
    }

    @get:JsonIgnore
    var value: V
        get() = _value
        set(value) {
            _value = value
            touch()
        }
    var time: DateTime = DateTime.now()
    var count = 0
    var location: Location? = null

    init {
        touch(true)
    }

    override fun equals(other: Any?): Boolean = if (other is Memory<*>) (_value == other._value) else false

    override fun hashCode(): Int = _value.hashCode()

    override fun toString(): String = "${this::class.simpleName}(value=$_value, count=$count, time=$time)"

    fun touch(init: Boolean = false) {
        count++
        if (AbstractDialogue.isRunning) {
            with(AbstractDialogue.run) {
                //if (!init && node.dialogue.clientLocation.isNotEmpty)
                //    location = node.dialogue.clientLocation
                time = context.turn.time
            }
        } else if (!init) {
            time = DateTime.now()
        }
    }

    class MemoryConverter<V: Any>: StdConverter<Memory<V>, Memory<V>>() {
        override fun convert(memory: Memory<V>): Memory<V> {
            return if (!memory.serializable) {
                Memorable.convert(memory._value).let {
                    val type = MemoryTypeIdResolver().idFromValue(it)
                    Memory(it, type, memory._origType, true).apply {
                        time = memory.time; count = memory.count; location = memory.location
                    } as Memory<V>
                }
            } else {
                memory
            }
        }
    }
}