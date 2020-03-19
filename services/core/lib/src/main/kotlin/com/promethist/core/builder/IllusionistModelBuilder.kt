package com.promethist.core.builder

import com.promethist.common.ObjectUtil
import com.promethist.core.model.Dialogue

class IllusionistModelBuilder(val apiEndpointUrl: String) : IntentModelBuilder {

    data class Model(val model: Model, val qa: Map<String, Item>) {
        data class Model(val name: String, val lang: String/*, val algorithm: String? = null*/)
        data class Item(val questions: List<String>, val answer: String)
    }

    override fun build(modelId: String, intents: List<Dialogue.Intent>) {
        TODO("Not yet implemented")
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val input = Model(Model.Model("modelX", "en"), mapOf(
                    "intent1" to Model.Item(listOf("yes", "ok"), "-1"),
                    "intent2" to Model.Item(listOf("no", "nope"), "-2")
            ))
            println(ObjectUtil.defaultMapper.writeValueAsString(input))
        }
    }
}