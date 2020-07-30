package com.promethist.core.dialogue.attribute

import com.promethist.core.Context
import com.promethist.core.dialogue.Dialogue
import kotlin.reflect.KProperty

class TempAttributeDelegate<V : Any>(default: (Context.() -> V)? = null) {

    private var value = object : ThreadLocal<V>() {
        override fun initialValue(): V? = default?.invoke(Dialogue.run.context)
    }

    operator fun getValue(thisRef: Any, property: KProperty<*>): V =
            value.get() ?: error("temp attribute ${property.name} is not initialized")

    open operator fun setValue(thisRef: Any, property: KProperty<*>, any: V) {
        value.set(any)
    }
}