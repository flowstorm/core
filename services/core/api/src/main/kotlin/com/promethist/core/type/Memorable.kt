package com.promethist.core.type

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "_class")
@JsonSubTypes(
        JsonSubTypes.Type(value = Memory::class, name = "Memory"),
        JsonSubTypes.Type(value = MemoryMutableSet::class, name = "MemoryMutableSet"),
        JsonSubTypes.Type(value = MemoryMutableList::class, name = "MemoryMutableList")
)
interface Memorable {

    companion object {
        fun pack(any: Any): Memorable =
                when {
                    any is Memorable -> any
                    Memory.canContain(any) -> Memory(any)
                    else -> error("unsupported memorable value type ${any::class.qualifiedName}")
                }
    }
}