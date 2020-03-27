package com.promethist.core.components

import com.fasterxml.jackson.annotation.JsonProperty
import com.promethist.core.Component
import com.promethist.core.Context
import com.promethist.core.Input
import com.promethist.core.builder.DialogueModelBuilder
import com.promethist.core.model.Turn
import org.slf4j.LoggerFactory
import javax.inject.Inject
import javax.ws.rs.client.Entity
import javax.ws.rs.client.WebTarget
import javax.ws.rs.core.GenericType

class Illusionist : Component {

    @Inject
    lateinit var webTarget: WebTarget

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun process(context: Context): Context {
        if (context.turn.dialogueStack.isEmpty()) {
            logger.info("processing IR - nothing to do")
            return context
        }

        val models = getModels(context.turn.dialogueStack.first)
        logger.info("processing IR with models $models")

        val request = Request(context.input.transcript.text, models.values.toList())
        val responses = webTarget.path("/multi_model").request().post(Entity.json(request), object : GenericType<List<Response>>() {})

        for (response in responses) {
            context.turn.input.classes.add(Input.Class(Input.Class.Type.Intent, response.answer, response.confidence))
        }

        return context
    }

    private fun getModels(frame: Turn.DialogueStackFrame): Map<String, String> = mapOf(
            frame.name to DialogueModelBuilder.md5(frame.name),
            "${frame.name}#${frame.nodeId}" to DialogueModelBuilder.md5("${frame.name}#${frame.nodeId}")
    )

    data class Response(
            val _id: String,
            val answer: String,
            val confidence: Float,
            val hit: String
    )

    data class Request(
            val query: String,
            val _ids: List<String>,
            @field:JsonProperty("denied_answers")
            val deniedAnswers: List<List<String?>?> = listOf(),
            @field:JsonProperty("allowed_answers")
            val allowedAnswers: List<List<String?>?> = listOf(),
            val n: Int = 3 //number of results ordered by confidence
    )
}