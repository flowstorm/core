package org.promethist.core.nlp

import com.fasterxml.jackson.annotation.JsonProperty
import org.glassfish.hk2.api.IterableProvider
import org.promethist.core.Component
import org.promethist.core.Context
import org.promethist.core.Input
import org.promethist.util.LoggerDelegate
import javax.inject.Inject
import javax.ws.rs.client.Entity
import javax.ws.rs.client.WebTarget
import javax.ws.rs.core.GenericType

class Illusionist : Component {

    @Inject
    lateinit var webTargets: IterableProvider<WebTarget>

    private val webTarget: WebTarget get() = webTargets.named("illusionist").get()

    private val logger by LoggerDelegate()

    override fun process(context: Context): Context {
        val models = context.intentModels
        if (models.isEmpty()) {
            logger.info("Processing IR - nothing to do")
            return context.pipeline.process(context)
        }

        context.logger.info("Processing IR with models $models")

        // The ER needs to be triggered before IR
        var text = context.input.transcript.text
        context.input.entityMap.values.flatten().forEach { if (it.required) text = text.replace(it.text, it.className) } // Better to replace by index
        val request = Request(text, models.map { it.id })
        val responses = webTarget.path("/multi_model").request().post(Entity.json(request), object : GenericType<List<Response>>() {})

        if (responses[0].answer == OUT_OF_DOMAIN_ACTION) {
            context.input.action = OUT_OF_DOMAIN_ACTION
        }

        for (response in responses) {
            if (response.answer != OUT_OF_DOMAIN_ACTION) {
                val name = response._id + "#" + response.answer

                context.turn.input.classes.add(Input.Class(Input.Class.Type.Intent, name, response.confidence))
            }
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
            val n: Int = 0 //number of maximum results, ordered by confidence, default 0 == no limit
    )

    companion object {
        const val OUT_OF_DOMAIN_ACTION = "outofdomain"
    }
}