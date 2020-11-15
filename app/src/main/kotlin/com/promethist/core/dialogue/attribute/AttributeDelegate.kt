package com.promethist.core.dialogue.attribute

import com.promethist.core.Context
import com.promethist.core.dialogue.AbstractDialogue
import com.promethist.core.dialogue.DateTimeUnit
import com.promethist.core.dialogue.plus
import com.promethist.core.type.DateTime
import com.promethist.core.type.Memory
import com.promethist.core.type.Memorable
import com.promethist.core.type.MemoryCollection
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.jvmName

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
                if (attribute.serializable) {
                    (attribute as Memory<Any>)._value = Memorable.unpack(attribute, thisRef::class.java)
                    attribute.serializable = false
                }
                attribute._value
            } else {
                (attribute as MemoryCollection<Any>).forEach {
                    it._value = Memorable.unpack(it, thisRef::class.java)
                }
                attribute
            } as V
        }

    open operator fun setValue(thisRef: AbstractDialogue, property: KProperty<*>, any: V) {
        attribute(namespace.invoke(), property.name) { Memorable.pack(any) }
    }
}
