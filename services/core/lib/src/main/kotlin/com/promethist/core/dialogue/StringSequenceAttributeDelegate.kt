package com.promethist.core.dialogue

import com.promethist.core.type.MemoryMutableSet
import kotlin.reflect.KProperty

class StringSequenceAttributeDelegate(
        private val entities: List<String>,
        scope: ContextualAttributeDelegate.Scope,
        namespace: (() -> String)? = null,
        val nextValue: (SequenceAttribute<String, String>.() -> String?)
) {
    private val attributeDelegate = ContextualAttributeDelegate(scope, MemoryMutableSet::class, namespace) { MemoryMutableSet<String>() }

    operator fun getValue(thisRef: Dialogue, property: KProperty<*>): SequenceAttribute<String, String> {
        val memories = attributeDelegate.getValue(thisRef, property)
        return object : SequenceAttribute<String, String>(entities, memories, nextValue) {
            override fun toMemoryValue(v: String) = v
        }
    }
}