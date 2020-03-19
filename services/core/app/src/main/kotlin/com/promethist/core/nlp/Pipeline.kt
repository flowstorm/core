package com.promethist.core.nlp

import org.glassfish.hk2.api.IterableProvider
import javax.inject.Inject

class Pipeline {

    @Inject
    lateinit var adapters: IterableProvider<Component>

    fun process(context: Context): Context {
        var processedContext = context

        for (adapter in adapters) {
            processedContext = adapter.process(processedContext)
        }

        return processedContext
    }
}