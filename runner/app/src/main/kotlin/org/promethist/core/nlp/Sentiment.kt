package org.promethist.core.nlp

import org.glassfish.hk2.api.IterableProvider
import org.promethist.core.Component
import org.promethist.core.Context
import org.promethist.util.LoggerDelegate
import javax.inject.Inject
import javax.ws.rs.client.Entity
import javax.ws.rs.client.WebTarget
import javax.ws.rs.core.GenericType

class Sentiment : Component {

    @Inject
    lateinit var webTargets: IterableProvider<WebTarget>

    private val webTarget: WebTarget get() = webTargets.named("triton").get()

    private val logger by LoggerDelegate()

    private val idx2Sentiment = mapOf(
            0 to "NEGATIVE",
            1 to "NEUTRAL",
            2 to "POSITIVE"
    )

    override fun process(context: Context): Context {

        logger.info("Calling Sentiment with input ${context.input}")

        val text = context.input.transcript.text
        val input = Input(listOf(text))
        val request = Request(listOf(input))
        val response = webTarget.path("/sentiment/infer").request().post(Entity.json(request), object : GenericType<Response>() {})
        response.outputs[0].data.forEachIndexed { i, score ->
            context.turn.input.classes.add(org.promethist.core.Input.Class(org.promethist.core.Input.Class.Type.Sentiment,
                            idx2Sentiment[i] ?: "UNKNOWN", score))
        }

        return context.pipeline.process(context)
    }

    data class Request(val inputs: List<Input>)

    data class Input(val data: List<String>){
        val name: String = "text"
        val datatype: String = "BYTES"
        val shape: List<Int> = listOf(1, 1)
    }

    data class Response(val outputs: List<Output>)

    data class Output(val data:List<Float>)
}