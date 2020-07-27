package com.promethist.core.dialogue.attribute

import com.promethist.core.Context
import com.promethist.core.dialogue.Dialogue
import com.promethist.core.model.Community
import kotlin.reflect.KClass

class CommunityAttributeDelegate<V: Any>(
        clazz: KClass<*>,
        private val communityName: String,
        namespace: (() -> String)? = null,
        default: (Context.() -> V)
) : AttributeDelegate<V>(clazz, namespace, default) {

    private val community get() = with (Dialogue.run.context) {
        communities.getOrPut(communityName) {
            communityResource.get(communityName) ?: Community(name = communityName).apply {
                communityResource.create(this)
            }
        }
    }

    override val attributes get() = community.attributes
}