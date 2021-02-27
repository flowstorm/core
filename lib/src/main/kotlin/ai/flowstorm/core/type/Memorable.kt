package ai.flowstorm.core.type

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import ai.flowstorm.common.ObjectUtil

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "_class")
@JsonSubTypes(
        JsonSubTypes.Type(value = Memory::class, name = "Memory"),
        JsonSubTypes.Type(value = MemoryMutableSet::class, name = "MemoryMutableSet"),
        JsonSubTypes.Type(value = MemoryMutableList::class, name = "MemoryMutableList")
)
interface Memorable {

    companion object {
        fun pack(any: Any): Memorable =
                when (any) {
                    is Memorable -> any
                    else -> Memory(any)
                }

        fun convert(any: Any): Any = when {
            Memory.canContainNatively(any) -> any
            any is Collection<*> -> any.map { mapOf(MemoryTypeIdResolver().idFromValue(it!!) to convert(it)) }
            any is Map<*, *> -> any.mapValues { mapOf(MemoryTypeIdResolver().idFromValue(it.value!!) to convert(it.value!!)) }
            else -> ObjectUtil.defaultMapper.convertValue(any, Map::class.java)
        }

        fun unpack(memory: Memorable, caller: Class<out Any> = Memorable::class.java) =
                when {
                    memory !is Memory<*> -> memory
                    memory._type != memory._origType -> ObjectUtil.defaultMapper.convertValue(memory._value,
                            MemoryTypeIdResolver.classFromId(memory._origType, caller))
                    Memory.canContainNatively(memory._value) -> memory._value
                    else -> unconvert(memory._value, caller)
                }

        private fun unconvert(any: Any, caller: Class<out Any> = Memorable::class.java): Any = when {
            Memory.canContainNatively(any) -> any
            any is Collection<*> -> any.map { e ->
                (e as Map<String, *>).let {
                    unconvert(ObjectUtil.defaultMapper.convertValue(it.values.first(),
                            MemoryTypeIdResolver.classFromId(it.keys.first(), caller)), caller)
                }
            }
            any is Map<*, *> -> any.mapValues { e ->
                (e.value as Map<String, *>).let {
                    unconvert(ObjectUtil.defaultMapper.convertValue(it.values.first(),
                            MemoryTypeIdResolver.classFromId(it.keys.first(), caller)), caller)
                }
            }
            else -> any
        }
    }
}