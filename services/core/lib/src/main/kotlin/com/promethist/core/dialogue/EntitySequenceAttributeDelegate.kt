package com.promethist.core.dialogue

import com.promethist.core.type.MemoryMutableSet
import com.promethist.core.type.NamedEntity
import kotlin.reflect.KProperty

class EntitySequenceAttributeDelegate<E: NamedEntity>(
        private val entities: List<E>,
        scope: ContextualAttributeDelegate.Scope,
        namespace: (() -> String)? = null,
        val nextValue: (SequenceAttribute<E, String>.() -> E?)
) {
    private val attributeDelegate = ContextualAttributeDelegate(scope, MemoryMutableSet::class, namespace) { MemoryMutableSet<String>() }

    operator fun getValue(thisRef: Dialogue, property: KProperty<*>): SequenceAttribute<E, String> {
        val memories = attributeDelegate.getValue(thisRef, property)
        return object : SequenceAttribute<E, String>(entities, memories, nextValue) {
            override fun toMemoryValue(entity: E) = entity.name
        }
    }
}