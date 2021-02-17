package org.promethist.core.dialogue.attribute

import org.promethist.core.dialogue.AbstractDialogue
import org.promethist.core.type.StringMutableSet
import kotlin.reflect.KProperty

class MapAttributeDelegate<E : Any>(
        val entities: Map<String, E>,
        scope: ContextualAttributeDelegate.Scope,
        namespace: (() -> String)
) {
    interface KeyMap<E> : MutableMap<String, E> {
        fun put(key: String): E?
    }

    private val attributeDelegate = ContextualAttributeDelegate(scope, MutableSet::class, namespace) { StringMutableSet() }

    operator fun getValue(thisRef: AbstractDialogue, property: KProperty<*>): KeyMap<E> {
        val keys = attributeDelegate.getValue(thisRef, property, false)
        return object : HashMap<String, E>(keys.map { it to entities[it] }.toMap()), KeyMap<E> {

            override fun get(key: String): E? = if (keys.contains(key)) entities[key] else null

            override fun put(key: String) =
                if (!keys.contains(key)) {
                    keys.add(key)
                    attributeDelegate.setValue(thisRef, property, keys)
                    super.put(key, entities[key] ?: error("missing entity with key $key"))
                } else {
                    null
                }

            override fun put(key: String, entity: E): E? =
                if (!keys.contains(key)) {
                    keys.add(key)
                    attributeDelegate.setValue(thisRef, property, keys)
                    super.put(key, entity)
                } else {
                    null
                }

            override fun remove(key: String): E? {
                keys.remove(key)
                attributeDelegate.setValue(thisRef, property, keys)
                return super<HashMap>.remove(key)
            }

            override fun clear() {
                keys.clear()
                attributeDelegate.setValue(thisRef, property, keys)
                super.clear()
            }
        }
    }
}