package ai.flowstorm.core.dialogue.attribute

import ai.flowstorm.core.Context
import ai.flowstorm.core.dialogue.DateTimeUnit
import ai.flowstorm.core.runtime.DialogueRuntime
import ai.flowstorm.core.type.Memorable
import kotlin.reflect.KClass

class ContextualAttributeDelegate<V: Any>(
        private val scope: Scope,
        clazz: KClass<*>,
        namespace: (() -> String),
        expiration: DateTimeUnit? = null,
        default: (Context.() -> V)
) : AttributeDelegate<V>(clazz, namespace, expiration, default) {

    // constructor for previously built models compatibility
    constructor(scope: Scope,
                clazz: KClass<*>,
                namespace: (() -> String),
                default: (Context.() -> V)) : this(scope, clazz, namespace, null, default)

    enum class Scope { Turn, Session, User, Client }

    override fun attribute(namespace: String, name: String, init: (Memorable?) -> Memorable) = with (DialogueRuntime.run.context) {
        val attributes = (when (scope) {
            Scope.Client -> getAttributes(name)
            Scope.User -> userProfile.attributes
            Scope.Session -> session.attributes
            else -> turn.attributes
        })[namespace]
        init(attributes[name]).also {
            attributes[name] = it
        }
    }
}