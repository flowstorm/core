package com.promethist.core.runtime

import com.fasterxml.jackson.annotation.JsonProperty
import com.promethist.core.Component
import com.promethist.core.Context
import com.promethist.core.Input
import com.promethist.util.LoggerDelegate
import javax.inject.Inject
import javax.ws.rs.client.Entity
import javax.ws.rs.client.WebTarget
import javax.ws.rs.core.GenericType

class Illusionist : Component {

    @Inject
    lateinit var webTarget: WebTarget

    private val logger by LoggerDelegate()

    override fun process(context: Context): Context {
        val models = context.intentModels
        if (models.isEmpty()) {
            logger.info("processing IR - nothing to do")
            return context.pipeline.process(context)
        }

        context.logger.info("processing IR with models $models")

        val request = Request(context.input.transcript.text, models.map { it.id })
        val responses = webTarget.path("/multi_model").request().post(Entity.json(request), object : GenericType<List<Response>>() {})

        for (response in responses) {
            val name = response._id + "#" + response.answer

            context.turn.input.classes.add(Input.Class(Input.Class.Type.Intent, name, response.confidence))
        }

        return context.pipeline.process(context)
    }

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
            @field:JsonProperty("use_threshold")
            val useThreshold: Boolean = true,
            val n: Int = 3 //number of maximum results for each model, ordered by confidence
    )
}