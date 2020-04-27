package com.promethist.core.dialogue

import kotlin.reflect.KProperty

class EntityMapAttributeDelegate<E>(
        val entities: Map<String, E>,
        scope: ContextualAttributeDelegate.Scope,
        namespace: (() -> String)? = null
) {
    interface EntityMap<E> : MutableMap<String, E> {
        fun put(key: String): E?
    }

    private val attributeDelegate = ContextualAttributeDelegate<MutableList<String>>(scope, MutableList::class, namespace, null)

    operator fun getValue(thisRef: Dialogue, property: KProperty<*>): EntityMap<E> {
        val keys = attributeDelegate.getValue(thisRef, property)
        return object : HashMap<String, E>(), EntityMap<E> {

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