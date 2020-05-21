package com.promethist.core.dialogue

import com.promethist.core.Context
import com.promethist.core.model.Community
import com.promethist.core.type.Attributes
import com.promethist.core.type.Value
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

class CommunityAttributeDelegate<V: Any>(
        clazz: KClass<*>,
        private val communityName: String,
        namespace: (() -> String)? = null,
        default: (Context.() -> V)? = null
) : AttributeDelegate<V>(clazz, namespace, default) {

    private val community get() = with (Dialogue.threadContext().context) {
        communityResource.get(communityName) ?: Community(name = communityName).apply {
            communityResource.create(this)
        }
    }

    override val attributes get() = community.attributes
}