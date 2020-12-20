package org.promethist.core.nlp

import org.glassfish.hk2.api.IterableProvider
import org.promethist.core.Component
import org.promethist.core.Context
import org.promethist.core.Input
import org.promethist.util.LoggerDelegate
import javax.inject.Inject
import javax.ws.rs.client.Entity
import javax.ws.rs.client.WebTarget
import javax.ws.rs.core.GenericType

class Cassandra : Component {

    @Inject
    lateinit var webTargets: IterableProvider<WebTarget>

    val webTarget: WebTarget get() = webTargets.named("cassandra").get()

    private val logger by LoggerDelegate()

    override fun process(context: Context): Context {
        if (context.session.dialogueStack.isEmpty()) {
            logger.info("Processing NER - nothing to do")
            return context.pipeline.process(context)
        }
        logger.info("Processing NER with input ${context.input}")

        try {
            context.turn.input =
                    webTarget.path("/all/default").queryParam("language", context.input.locale.toString())
                            .request().post(Entity.json(context.input), object : GenericType<Input>() {})
        } catch (t:Throwable) {
            //TODO we are not using cassandra - exception should not block pipeline processing
            context.logger.error("Call to Cassandra failed: " + t.message)
        }
        return context.pipeline.process(context)
    }
}