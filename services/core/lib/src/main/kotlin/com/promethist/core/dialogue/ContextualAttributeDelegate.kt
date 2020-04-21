package com.promethist.core.dialogue

import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import com.promethist.core.Context

class ContextualAttributeDelegate<V: Any>(
        private val scope: Scope,
        private val clazz: KClass<*>,
        private val namespace: (() -> String)? = null,
        private val default: (Context.() -> V)? = null
) {
    enum class Scope { Turn, Session, User }

    private fun attributes(context: Context) = with (context) {
        when (scope) {
            Scope.Session -> session.attributes
            Scope.User -> userProfile.attributes
            else -> turn.attributes
        }
    }

    private fun key(name: String) = namespace?.let { it() + ".$name" } ?: name

    operator fun getValue(thisRef: Dialogue, property: KProperty<*>): V = with (Dialogue.threadContext().context) {
        val eval = { default!!.invoke(this) }
        attributes(this).put(key(property.name), clazz, if (default != null) eval else null) { value } as V
    }

    operator fun setValue(thisRef: Dialogue, property: KProperty<*>, any: V) = with (Dialogue.threadContext().context) {
        val eval = { default!!.invoke(this) }
        attributes(this).put(key(property.name), clazz, if (default != null) eval else null) { value = any; Unit }
    }
}