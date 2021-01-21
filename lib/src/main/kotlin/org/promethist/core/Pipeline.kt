package org.promethist.core

import java.util.*

interface Pipeline {

    val components: LinkedList<Component>

    fun process(context: Context): Context

    class PipelineComponentFailure(component: Component, cause: Throwable) : Throwable("Pipeline component ${component::class.simpleName} failed", cause)
}