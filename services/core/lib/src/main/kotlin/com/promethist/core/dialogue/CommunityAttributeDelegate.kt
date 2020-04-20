package com.promethist.core.dialogue

import com.promethist.core.Context
import com.promethist.core.model.Community
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

class CommunityAttributeDelegate<V: Any>(
        private val clazz: KClass<*>,
        private val communityName: String,
        private val namespace: (() -> String)? = null,
        private val default: (Context.() -> V)? = null
) {
    private val community get() = with (Dialogue.threadContext().context) {
        communityResource.get(communityName) ?: Community(name = communityName).apply {
            communityResource.create(this)
        }
    }

    private fun key(name: String) = namespace?.let { it() + ".$name" } ?: name

    operator fun getValue(thisRef: Dialogue, property: KProperty<*>): V = with (Dialogue.threadContext().context) {
        val eval = { default!!.invoke(this) }
        community.attributes.put(key(property.name), clazz, if (default != null) eval else null) { value } as V
    }

    operator fun setValue(thisRef: Dialogue, property: KProperty<*>, any: V) = with (Dialogue.threadContext().context) {
        val eval = { default!!.invoke(this) }
        with (community) {
            attributes.put(key(property.name), clazz, if (default != null) eval else null) { value = any; Unit }
            communityResource.update(this)
        }
    }
}