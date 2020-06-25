package com.promethist.core.model

import com.promethist.core.type.MutablePropertyMap
import org.litote.kmongo.Id
import org.litote.kmongo.newId

open class Application(
        open var _id: Id<Application> = newId(),
        open var name: String,
        open var dialogueName: String,
        open var voice: Voice? = null,
        open var public: Boolean = false,
        open var icon: String? = null,
        open var startCondition: StartCondition = StartCondition(StartCondition.Type.OnAction, "\$intro"),
        open var dialogueEngine: String = "helena",
        open var properties: MutablePropertyMap = mutableMapOf()
) {
    data class StartCondition(
            var type: Type,
            var condition: String
    ) {
        enum class Type {
            OnAction,
            OnTrigger
        }
    }
}