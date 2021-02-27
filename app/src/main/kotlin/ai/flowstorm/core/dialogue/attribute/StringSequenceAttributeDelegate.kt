package ai.flowstorm.core.dialogue.attribute

import ai.flowstorm.core.dialogue.AbstractDialogue
import ai.flowstorm.core.type.MemoryMutableSet
import kotlin.reflect.KProperty

class StringSequenceAttributeDelegate(
        private val entities: List<String>,
        scope: ContextualAttributeDelegate.Scope,
        namespace: (() -> String),
        val nextValue: (SequenceAttribute<String, String>.() -> String?)
) {
    private val attributeDelegate = ContextualAttributeDelegate(scope, MemoryMutableSet::class, namespace) { MemoryMutableSet<String>() }

    operator fun getValue(thisRef: AbstractDialogue, property: KProperty<*>): SequenceAttribute<String, String> {
        val memories = attributeDelegate.getValue(thisRef, property, false)
        return object : SequenceAttribute<String, String>(entities, memories, nextValue) {
            override fun toMemoryValue(e: String) = e
        }
    }
}