package com.promethist.core

import org.glassfish.hk2.api.IterableProvider
import java.util.*
import javax.inject.Inject

class PipelineFactory {

    class PipelineImpl(override val components: LinkedList<Component>) : Pipeline {

        override fun process(context: Context): Context {
            var processedContext = context
            if (components.isNotEmpty()) {
                val component = components.pop()
                try {
                    processedContext = component.process(processedContext)
                } catch (e: Throwable) {
                    throw Exception("NLP component failed: ${component::class.simpleName}: ${e.message}", e)
                }
            }
            return processedContext
        }
    }

    @Inject
    lateinit var bindedComponents: IterableProvider<Component>

    fun createPipeline(): Pipeline = PipelineImpl(LinkedList(bindedComponents.toList()))
}