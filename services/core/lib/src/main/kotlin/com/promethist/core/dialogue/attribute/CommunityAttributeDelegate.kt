package com.promethist.core.dialogue.attribute

import com.promethist.core.Context
import com.promethist.core.dialogue.AbstractDialogue
import com.promethist.core.dialogue.DateTimeUnit
import com.promethist.core.model.Community
import kotlin.reflect.KClass

class CommunityAttributeDelegate<V: Any>(
        clazz: KClass<*>,
        private val communityName: String,
        namespace: (() -> String),
        expiration: DateTimeUnit? = null,
        default: (Context.() -> V)
) : AttributeDelegate<V>(clazz, namespace, expiration, default) {

    private val community get() = with (AbstractDialogue.run.context) {
        communities.getOrPut(communityName) {
            communityResource.get(communityName, organizationId = session.properties["organization_id"] as String) ?: Community(name = communityName, organization_id = session.properties["organization_id"] as String?).apply {
                communityResource.create(this)
            }
        }
    }

    override val attributes get() = community.attributes
}