package ai.flowstorm.core.dialogue.attribute

import ai.flowstorm.core.Context
import ai.flowstorm.core.runtime.DialogueRuntime
import kotlin.reflect.KProperty

class TempAttributeDelegate<V : Any>(default: (Context.() -> V)? = null) {

    private var value = object : ThreadLocal<V>() {
        override fun initialValue(): V? = default?.invoke(DialogueRuntime.run.context)
    }

    operator fun getValue(thisRef: Any, property: KProperty<*>): V =
            value.get() ?: error("temp attribute ${property.name} is not initialized")

    open operator fun setValue(thisRef: Any, property: KProperty<*>, any: V) {
        value.set(any)
    }
}