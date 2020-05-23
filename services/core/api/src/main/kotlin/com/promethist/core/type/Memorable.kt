package com.promethist.core.type

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "_class")
@JsonSubTypes(
        JsonSubTypes.Type(value = Memory::class, name = "Value"),
        JsonSubTypes.Type(value = MemoryMutableSet::class, name = "ValueMutableSet"),
        JsonSubTypes.Type(value = MemoryMutableList::class, name = "ValueMutableList")
)
interface Memorable {

    companion object {
        fun pack(any: Any): Memorable =
                when {
                    any is Memorable -> any
                    Memory.canContain(any) -> Memory(any)
                    else -> error("unsupported type ${any::class.qualifiedName}")
                }
    }
}