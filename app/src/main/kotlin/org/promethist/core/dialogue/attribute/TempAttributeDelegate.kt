package org.promethist.core.dialogue.attribute

import org.promethist.core.Context
import org.promethist.core.dialogue.AbstractDialogue
import kotlin.reflect.KProperty

class TempAttributeDelegate<V : Any>(default: (Context.() -> V)? = null) {

    private var value = object : ThreadLocal<V>() {
        override fun initialValue(): V? = default?.invoke(AbstractDialogue.run.context)
    }

    operator fun getValue(thisRef: Any, property: KProperty<*>): V =
            value.get() ?: error("temp attribute ${property.name} is not initialized")

    open operator fun setValue(thisRef: Any, property: KProperty<*>, any: V) {
        value.set(any)
    }
}