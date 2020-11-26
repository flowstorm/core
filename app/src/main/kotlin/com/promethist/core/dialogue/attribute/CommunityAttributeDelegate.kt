package com.promethist.core.dialogue.attribute

import com.promethist.core.Context
import com.promethist.core.dialogue.AbstractDialogue
import com.promethist.core.model.Community
import com.promethist.core.type.Memorable
import kotlin.reflect.KClass

class CommunityAttributeDelegate<V: Any>(
        clazz: KClass<*>,
        private val communityName: String,
        namespace: (() -> String),
        default: (Context.() -> V)
) : AttributeDelegate<V>(clazz, namespace, null, default) {

    private val community get() = with (AbstractDialogue.run.context) {
        communities.getOrPut(communityName) {
            communityStorage.get(communityName, organizationId = session.properties["organization_id"] as String) ?: Community(name = communityName, organization_id = session.properties["organization_id"] as String?).apply {
                communityStorage.create(this)
            }
        }
    }

    override fun attribute(namespace: String, name: String, lambda: (Memorable?) -> Memorable): Memorable {
        val attributes = community.attributes[namespace]
        val attribute = attributes[name]
        return lambda(attribute)?.apply {
            attributes[name] = this
        }
    }
}