package com.promethist.core.dialogue.attribute

import kotlin.reflect.KClass
import com.promethist.core.Context
import com.promethist.core.dialogue.AbstractDialogue
import com.promethist.core.dialogue.DateTimeUnit

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

    enum class Scope { Turn, Session, User }

    override val attributes get() = with (AbstractDialogue.run.context) {
        when (scope) {
            Scope.Session -> session.attributes
            Scope.User -> userProfile.attributes
            else -> turn.attributes
        }
    }
}