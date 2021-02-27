package ai.flowstorm.core.dialogue.attribute

import ai.flowstorm.core.dialogue.AbstractDialogue
import ai.flowstorm.core.type.MemoryMutableSet
import ai.flowstorm.core.type.NamedEntity
import kotlin.reflect.KProperty

class EntitySequenceAttributeDelegate<E: NamedEntity>(
        private val entities: List<E>,
        scope: ContextualAttributeDelegate.Scope,
        namespace: (() -> String),
        val nextValue: (SequenceAttribute<E, String>.() -> E?)
) {
    private val attributeDelegate = ContextualAttributeDelegate(scope, MemoryMutableSet::class, namespace) { MemoryMutableSet<String>() }

    operator fun getValue(thisRef: AbstractDialogue, property: KProperty<*>): SequenceAttribute<E, String> {
        val memories = attributeDelegate.getValue(thisRef, property, false)
        return object : SequenceAttribute<E, String>(entities, memories, nextValue) {
            override fun toMemoryValue(e: E) = e.name
        }
    }
}