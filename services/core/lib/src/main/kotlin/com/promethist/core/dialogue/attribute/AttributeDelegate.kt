package com.promethist.core.dialogue.attribute

import com.promethist.core.Context
import com.promethist.core.dialogue.AbstractDialogue
import com.promethist.core.dialogue.DateTimeUnit
import com.promethist.core.dialogue.plus
import com.promethist.core.type.DateTime
import com.promethist.core.type.Memory
import com.promethist.core.type.Memorable
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.isSubclassOf

abstract class AttributeDelegate<V: Any>(private val clazz: KClass<*>, val namespace: (() -> String), private val expiration: DateTimeUnit? = null, val default: (Context.() -> V)) {

    abstract fun attribute(namespace: String, name: String, lambda: (Memorable?) -> Memorable): Memorable

    operator fun getValue(thisRef: AbstractDialogue, property: KProperty<*>): V =
        attribute(namespace.invoke(), property.name) { attribute ->
            if (attribute == null || (expiration != null && attribute is Memory<*> && attribute.time + expiration < DateTime.now())) {
                Memorable.pack(default.invoke(AbstractDialogue.run.context))
            } else {
                attribute
            }
        }.let { attribute ->
            if (!clazz.isSubclassOf(Memory::class) && attribute is Memory<*>) {
                attribute.value
            } else {
                attribute
            } as V
        }

    open operator fun setValue(thisRef: AbstractDialogue, property: KProperty<*>, any: V) {
        attribute(namespace.invoke(), property.name) { Memorable.pack(any) }
    }
}
