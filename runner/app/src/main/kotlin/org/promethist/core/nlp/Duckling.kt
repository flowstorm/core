package org.promethist.core.nlp

import org.glassfish.hk2.api.IterableProvider
import org.promethist.core.Component
import org.promethist.core.Context
import org.promethist.core.type.DucklingEntity
import org.promethist.util.LoggerDelegate
import javax.inject.Inject
import javax.ws.rs.client.Entity
import javax.ws.rs.client.WebTarget
import javax.ws.rs.core.Form
import javax.ws.rs.core.GenericType

class Duckling: Component {
    @Inject
    lateinit var webTargets: IterableProvider<WebTarget>

    val webTarget: WebTarget get() = webTargets.named("duckling").get()

    private val logger by LoggerDelegate()

    override fun process(context: Context): Context {
        logger.info("calling Duckling with input ${context.input}")

        try {
            val response = webTarget.request().post(
                    Entity.form(Form()
                            .param("locale", context.input.locale.toString())
                            .param("tz", context.input.zoneId.id)
                            .param("text", context.input.transcript.text)), object : GenericType<List<DucklingEntity>>() {})
            for (entity in response) {
                context.turn.input.entityMap.getOrPut(entity.className, { mutableListOf() }).add(entity)
            }
        } catch (t:Throwable) {
            // The exception should not block pipeline processing
            context.logger.error("Call to Duckling failed: " + t.message)
        }
        return context.pipeline.process(context)
    }
}