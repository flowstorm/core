package com.promethist.core.runtime

import com.promethist.core.Component
import com.promethist.core.Context
import com.promethist.core.Input
import com.promethist.util.LoggerDelegate
import javax.inject.Inject
import javax.ws.rs.client.Entity
import javax.ws.rs.client.WebTarget
import javax.ws.rs.core.GenericType

class Cassandra : Component {

    @Inject
    lateinit var webTarget: WebTarget

    private val logger by LoggerDelegate()

    override fun process(context: Context): Context {
        if (context.session.dialogueStack.isEmpty()) {
            logger.info("processing NER - nothing to do")
            return context.pipeline.process(context)
        }
        logger.info("processing NER with input ${context.input}")

        try {
            context.turn.input =
                    webTarget.path("/ner/default").queryParam("language", context.input.locale.toString())
                            .request().post(Entity.json(context.input), object : GenericType<Input>() {})
        } catch (t:Throwable) {
            //TODO we are not using cassandra - exception should not block pipeline processing
            context.logger.error("Call to Cassandra failed: " + t.message)
        }
        return context.pipeline.process(context)
    }
}