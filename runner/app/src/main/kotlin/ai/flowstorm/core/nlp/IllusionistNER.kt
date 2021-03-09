package ai.flowstorm.core.nlp

import org.glassfish.hk2.api.IterableProvider
import ai.flowstorm.core.Component
import ai.flowstorm.core.Context
import ai.flowstorm.core.Input
import ai.flowstorm.util.LoggerDelegate
import javax.inject.Inject
import javax.ws.rs.client.Entity
import javax.ws.rs.client.WebTarget
import javax.ws.rs.core.GenericType

class IllusionistNER : Component {

    @Inject
    lateinit var webTargets: IterableProvider<WebTarget>

    val webTarget: WebTarget get() = webTargets.named("illusionist").get()

    private val logger by LoggerDelegate()

    override fun process(context: Context): Context {
        logger.info("Processing NER with input ${context.input}")

        try {
            val response = webTarget.path("/entity/query/default_en").queryParam("language", context.input.locale.toString())
                .request().post(Entity.json(context.input), object : GenericType<Response>() {})
            context.turn.input.tokens.clear()
            context.turn.input.tokens.addAll(response.tokens)
        } catch (t:Throwable) {
            context.logger.error("Call to Illusionist (entity) failed: " + t.message)
        }
        return context.pipeline.process(context)
    }

    data class Response(val tokens: List<Input.Token>)
}