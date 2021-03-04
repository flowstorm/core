package ai.flowstorm.core.runtime

import ai.flowstorm.core.Component
import ai.flowstorm.core.Context
import ai.flowstorm.core.Pipeline
import org.glassfish.hk2.api.IterableProvider
import ai.flowstorm.core.Pipeline.PipelineComponentFailure
import ai.flowstorm.util.LoggerDelegate
import java.util.*
import javax.inject.Inject

class PipelineFactory {

    class PipelineImpl(override val components: LinkedList<Component>) : Pipeline {

        val logger by LoggerDelegate()

        override fun process(context: Context): Context {

            var processedContext = context
            if (components.isNotEmpty()) {
                val component = components.pop()
                    logger.info("NLP component start ${component::class.simpleName}")
                try {
                    processedContext = component.process(processedContext)
                } catch (e: Throwable) {
                    if (e is PipelineComponentFailure) throw e
                    throw PipelineComponentFailure(component, e)
                }
            }
            return processedContext
        }
    }
    @Inject
    lateinit var bindedComponents: IterableProvider<Component>

    fun createPipeline(): Pipeline = PipelineImpl(LinkedList(bindedComponents.toList().reversed()))
}