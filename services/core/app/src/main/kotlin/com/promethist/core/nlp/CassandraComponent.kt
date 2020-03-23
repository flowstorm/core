package com.promethist.core.nlp

import org.slf4j.LoggerFactory
import javax.inject.Inject
import javax.ws.rs.client.Entity
import javax.ws.rs.client.WebTarget
import javax.ws.rs.core.GenericType

class CassandraComponent : Component {

    @Inject
    lateinit var webTarget: WebTarget

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun process(context: Context): Context {
        if (context.turn.dialogueStack.isEmpty()) {
            logger.info("processing NER - nothing to do")
            return context
        } else {
            logger.info("processing NER")
        }

        context.turn.input =
                webTarget.path("/ner/ner3").request().post(Entity.json(context.turn.input), object : GenericType<Input>() {})

        return context
    }
}