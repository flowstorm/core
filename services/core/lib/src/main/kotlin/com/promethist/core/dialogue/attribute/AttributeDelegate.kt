package com.promethist.core.dialogue.attribute

import com.promethist.core.Context
import com.promethist.core.dialogue.AbstractDialogue
import com.promethist.core.dialogue.DateTimeUnit
import com.promethist.core.dialogue.plus
import com.promethist.core.type.Attributes
import com.promethist.core.type.DateTime
import com.promethist.core.type.Memory
import com.promethist.core.type.Memorable
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.isSubclassOf

abstract class AttributeDelegate<V: Any>(private val clazz: KClass<*>, val namespace: (() -> String), private val expiration: DateTimeUnit? = null, val default: (Context.() -> V)) {

    abstract val attributes: Attributes

    operator fun getValue(thisRef: AbstractDialogue, property: KProperty<*>): V =
        attributes[namespace.invoke()].let {
            var attribute = it[property.name]
            if (attribute == null || (expiration != null && attribute is Memory<*> && attribute.time + expiration < DateTime.now())) {
                attribute = Memorable.pack(default.invoke(AbstractDialogue.run.context))
                it[property.name] = attribute
            }
            if (!clazz.isSubclassOf(Memory::class) && attribute is Memory<*>) {
                attribute.value
            } else {
                attribute
            } as V
        }

    open operator fun setValue(thisRef: AbstractDialogue, property: KProperty<*>, any: V) {
        attributes[namespace.invoke()][property.name] = Memorable.pack(any)
    }
}
