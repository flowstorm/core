package com.promethist.core.runtime

import com.promethist.core.Component
import com.promethist.core.Context
import com.promethist.core.nlu.NumericEntity
import com.promethist.util.LoggerDelegate
import javax.inject.Inject
import javax.ws.rs.client.Entity
import javax.ws.rs.client.WebTarget
import javax.ws.rs.core.Form
import javax.ws.rs.core.GenericType

class Duckling: Component {
    @Inject
    lateinit var webTarget: WebTarget

    private val logger by LoggerDelegate()

    override fun process(context: Context): Context {
        logger.info("calling Duckling with input ${context.input}")

        try {
            val response = webTarget.request().post(
                    Entity.form(Form()
                            .param("locale", context.input.locale.toString())
                            .param("tz", context.input.zoneId.id)
                            .param("text", context.input.transcript.text)), object : GenericType<List<NumericEntity>>() {})
            for (entity in response) {
                if (!context.turn.input.entityMap.containsKey(entity.className)) {
                    context.turn.input.entityMap[entity.className] = mutableListOf()
                }
                context.turn.input.entityMap[entity.className]!!.add(entity)
            }
        } catch (t:Throwable) {
            // The exception should not block pipeline processing
            context.logger.error("Call to Duckling failed: " + t.message)
        }
        return context.pipeline.process(context)
    }
}