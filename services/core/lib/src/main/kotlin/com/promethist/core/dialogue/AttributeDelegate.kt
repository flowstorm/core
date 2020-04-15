package com.promethist.core.dialogue

import kotlin.reflect.KClass
import kotlin.reflect.KProperty

class AttributeDelegate<V: Any>(
        private val scope: Scope,
        private val clazz: KClass<*>,
        private val namespace: (() -> String)? = null,
        private val default: (() -> V)? = null
) {
    enum class Scope { Turn, Session, Profile }

    private val attributes get() = with (Dialogue.threadContext().context) {
        when (scope) {
            Scope.Session -> session.attributes
            Scope.Profile -> profile.attributes
            else -> turn.attributes
        }
    }

    private fun key(name: String) = if (namespace != null) namespace!!() + ".$name" else name

    operator fun getValue(thisRef: Dialogue, property: KProperty<*>): V =
            attributes.put(key(property.name), clazz, default) { value } as V

    operator fun setValue(thisRef: Dialogue, property: KProperty<*>, any: V) =
            attributes.put(key(property.name), clazz, default) { value = any; Unit }
}