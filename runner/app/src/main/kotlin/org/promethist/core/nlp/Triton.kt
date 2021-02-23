package org.promethist.core.nlp

import org.glassfish.hk2.api.IterableProvider
import org.promethist.core.Component
import org.promethist.core.Context
import org.promethist.core.model.Sentiment
import org.promethist.util.LoggerDelegate
import javax.inject.Inject
import javax.ws.rs.client.Entity
import javax.ws.rs.client.WebTarget
import javax.ws.rs.core.GenericType

class Triton : Component {

    @Inject
    lateinit var webTargets: IterableProvider<WebTarget>

    private val webTarget: WebTarget get() = webTargets.named("triton").get()

    private val logger by LoggerDelegate()

    private val idx2Sentiment = mapOf(
            0 to Sentiment.NEGATIVE,
            1 to Sentiment.NEUTRAL,
            2 to Sentiment.POSITIVE
    )

    override fun process(context: Context): Context {
        sentiment(context)
        return context.pipeline.process(context)
    }

    private fun sentiment(context: Context) {
        logger.info("Calling Sentiment with input ${context.input}")

        val text = context.input.transcript.text
        val input = Input(listOf(text))
        val request = Request(listOf(input))
        try {
            val response = webTarget.path("/sentiment/infer").request().post(Entity.json(request), object : GenericType<Response>() {})
            val sentimentClasses = mutableListOf<org.promethist.core.Input.Class>()
            response.outputs[0].data.forEachIndexed { i, score ->
                sentimentClasses.add(org.promethist.core.Input.Class(org.promethist.core.Input.Class.Type.Sentiment,
                        (idx2Sentiment[i] ?: Sentiment.UNKNOWN).toString(), score))
            }
            context.turn.input.classes.addAll(
                    sentimentClasses.sortedWith(compareByDescending(org.promethist.core.Input.Class::score)))
        } catch (t: Throwable) {
            context.logger.error("Call to Triton sentiment model failed: " + t.message)
        }
    }

    data class Request(val inputs: List<Input>)

    data class Input(val data: List<String>) {
        val name: String = "text"
        val datatype: String = "BYTES"
        val shape: List<Int> = listOf(1, 1)
    }

    data class Response(val outputs: List<Output>)

    data class Output(val data: List<Float>)
}