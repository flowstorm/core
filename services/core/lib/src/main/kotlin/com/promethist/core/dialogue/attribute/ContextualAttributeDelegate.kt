package com.promethist.core.dialogue.attribute

import kotlin.reflect.KClass
import com.promethist.core.Context
import com.promethist.core.dialogue.AbstractDialogue
import com.promethist.core.dialogue.DateTimeUnit
import com.promethist.core.type.Memorable

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

    override fun attribute(namespace: String, name: String, lambda: (Memorable?) -> Memorable) = with (AbstractDialogue.run.context) {
        val attributes = (when (scope) {
            Scope.Client -> {
                if (isClientUserAttribute(name))
                    userProfile.attributes
                else
                    session.attributes
            }
            Scope.User -> userProfile.attributes
            Scope.Session -> session.attributes
            else -> turn.attributes
        })[namespace]
        val attribute = attributes[name]
        lambda(attribute)?.apply {
            attributes[name] = this
        }
    }

    companion object {
        fun isClientUserAttribute(name: String) = name.startsWith("clientUser") || name == "clientLocation"
    }
}