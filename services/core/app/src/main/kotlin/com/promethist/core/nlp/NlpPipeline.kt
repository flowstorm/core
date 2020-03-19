package com.promethist.core.nlp

import com.promethist.core.Context
import org.glassfish.hk2.api.IterableProvider
import javax.inject.Inject

class NlpPipeline {

    @Inject
    lateinit var adapters: IterableProvider<NlpAdapter>

    fun process(context: Context): Context {
        var processedContext = context

        for (adapter in adapters) {
            processedContext = adapter.process(processedContext)
        }

        return processedContext
    }
}