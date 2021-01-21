package org.promethist.core

import java.util.*
import kotlin.reflect.KType
import kotlin.reflect.full.createType

interface Pipeline {

    val components: LinkedList<Component>

    fun process(context: Context): Context

    fun removeComponent(type:KType) {
        components.removeIf { it::class.createType() == type }
    }

    class PipelineComponentFailure(component: Component, cause: Throwable) : Throwable("Pipeline component ${component::class.simpleName} failed", cause)
}