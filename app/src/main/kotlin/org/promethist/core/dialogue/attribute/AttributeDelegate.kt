package org.promethist.core.dialogue.attribute

import org.promethist.core.Context
import org.promethist.core.dialogue.AbstractDialogue
import org.promethist.core.dialogue.DateTimeUnit
import org.promethist.core.dialogue.plus
import org.promethist.core.type.DateTime
import org.promethist.core.type.Memorable
import org.promethist.core.type.Memory
import org.promethist.core.type.MemoryCollection
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
