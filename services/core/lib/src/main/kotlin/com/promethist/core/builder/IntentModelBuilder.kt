package com.promethist.core.builder

import com.promethist.core.model.Dialogue

interface IntentModelBuilder {

    fun build(modelId: String, intents: List<Dialogue.Intent>)
}