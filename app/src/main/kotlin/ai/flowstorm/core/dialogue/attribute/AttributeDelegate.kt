package ai.flowstorm.core.dialogue.attribute

import ai.flowstorm.core.Context
import ai.flowstorm.core.dialogue.AbstractDialogue
import ai.flowstorm.core.dialogue.DateTimeUnit
import ai.flowstorm.core.dialogue.plus
import ai.flowstorm.core.runtime.DialogueRuntime
import ai.flowstorm.core.type.DateTime
import ai.flowstorm.core.type.Memorable
import ai.flowstorm.core.type.Memory
import ai.flowstorm.core.type.MemoryCollection
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.jvmErasure

abstract class AttributeDelegate<V: Any>(private val clazz: KClass<*>, val namespace: (() -> String), private val expiration: DateTimeUnit? = null, val default: (Context.() -> V)) {

    abstract fun attribute(namespace: String, name: String, lambda: (Memorable?) -> Memorable): Memorable

    fun getValue(thisRef: AbstractDialogue, property: KProperty<*>, valueTypeControl: Boolean): V =
        attribute(namespace.invoke(), property.name) { attribute ->
            if (attribute == null || (expiration != null && attribute is Memory<*> && attribute.time + expiration < DateTime.now())) {
                Memorable.pack(default.invoke(DialogueRuntime.run.context))
            } else {
                attribute
            }
        }.let { attribute ->
            try {
                if (attribute is Memory<*>) {
                    if (attribute.serializable) {
                        (attribute as Memory<Any>)._value = Memorable.unpack(attribute, thisRef::class.java)
                        attribute.serializable = false
                    }
                    if (!clazz.isSubclassOf(Memory::class)) {
                        attribute._value
                    } else {
                        attribute
                    }
                } else {
                    (attribute as MemoryCollection<Any>).forEach {
                        if (it.serializable) {
                            it._value = Memorable.unpack(it, thisRef::class.java)
                            it.serializable = false
                        }
                    }
                    attribute
                }.let {
                    val propClass = property.returnType.jvmErasure
                    val valueClass = it::class
                    if (!valueTypeControl || valueClass.isSubclassOf(propClass)) {
                        it as V
                    } else {
                        restoreDefault(thisRef, property, "Attribute ${property.name} value type mismatch " +
                                "(expected ${propClass.qualifiedName}, got ${valueClass.qualifiedName}, using default value instead)")
                    }
                }
            } catch (e: Exception) {
                restoreDefault(thisRef, property, "Cannot deserialize attribute ${property.name}. Reason: ${e::class.java.name}: ${e.message} (using default value instead)")
            }
        }

    operator fun getValue(thisRef: AbstractDialogue, property: KProperty<*>): V = getValue(thisRef, property, true)

    open operator fun setValue(thisRef: AbstractDialogue, property: KProperty<*>, any: V) {
        attribute(namespace.invoke(), property.name) { Memorable.pack(any) }
    }

    private fun restoreDefault(thisRef: AbstractDialogue, property: KProperty<*>, message: String) = with (DialogueRuntime.run) {
        context.logger.warn(message)
        val defaultValue = default.invoke(context)
        setValue(thisRef, property, defaultValue)
        defaultValue
    }
}
