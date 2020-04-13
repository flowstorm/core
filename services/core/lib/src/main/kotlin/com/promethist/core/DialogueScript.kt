package com.promethist.core

import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

open class DialogueScript {

    enum class AttributeScope { Turn, Session, Profile }

    inner class AttributeDelegate<V: Any>(
            private val scope: AttributeScope,
            private val clazz: KClass<*>,
            private val namespace: String,
            private val default: (() -> V)? = null
    ) {
        private val attributes get() = with(currentContext.get() ?: error("out of context")) {
            when (scope) {
                AttributeScope.Session -> session.attributes
                AttributeScope.Profile -> profile.attributes
                else -> turn.attributes
            }
        }

        operator fun getValue(thisRef: Dialogue, property: KProperty<*>): V =
                attributes.put("$namespace.${property.name}", clazz, default) { value } as V

        operator fun setValue(thisRef: Dialogue, property: KProperty<*>, any: V) =
                attributes.put("$namespace.${property.name}", clazz, default) { value = any; Unit }
    }

    val now get() = LocalDateTime.now()
    val today get() = LocalDate.now()

    private val currentContext = ThreadLocal<Context>()

    fun withCurrentContext(context: Context, lambda: () -> Any?): Any? =
            try {
                currentContext.set(context)
                lambda()
            } finally {
                currentContext.remove()
            }

}