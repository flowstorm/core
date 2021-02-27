package ai.flowstorm.core.dialogue.attribute

import ai.flowstorm.core.Context
import ai.flowstorm.core.dialogue.AbstractDialogue
import ai.flowstorm.core.model.Community
import ai.flowstorm.core.type.Memorable
import kotlin.reflect.KClass

class CommunityAttributeDelegate<V: Any>(
        clazz: KClass<*>,
        private val communityName: String,
        namespace: (() -> String),
        default: (Context.() -> V)
) : AttributeDelegate<V>(clazz, namespace, null, default) {

    private val community get() = with (AbstractDialogue.run.context) {
        communities.getOrPut(communityName) {
            communityRepository.get(communityName, spaceId = session.space_id.toString()) ?: Community(name = communityName, space_id = session.space_id.toString()).apply {
                communityRepository.create(this)
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