package com.promethist.core.dialogue

import kotlin.reflect.KClass
import com.promethist.core.Context

class ContextualAttributeDelegate<V: Any>(
        private val scope: Scope,
        clazz: KClass<*>,
        namespace: (() -> String)? = null,
        default: (Context.() -> V)
) : AttributeDelegate<V>(clazz, namespace, default) {

    enum class Scope { Turn, Session, User }

    override val attributes get() = with (Dialogue.threadContext().context) {
        when (scope) {
            Scope.Session -> session.attributes
            Scope.User -> userProfile.attributes
            else -> turn.attributes
        }
    }
}