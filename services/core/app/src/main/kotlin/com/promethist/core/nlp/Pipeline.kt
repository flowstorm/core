package com.promethist.core.nlp

import org.glassfish.hk2.api.IterableProvider
import javax.inject.Inject

class Pipeline {

    @Inject
    lateinit var components: IterableProvider<Component>

    fun process(context: Context): Context {
        var processedContext = context

        for (component in components) {
            try {
                processedContext = component.process(processedContext)
            } catch (e: Throwable) {
                throw Exception("NLP component failed: ${component::class.simpleName}: ${e.message}", e)
            }
        }

        return processedContext
    }
}