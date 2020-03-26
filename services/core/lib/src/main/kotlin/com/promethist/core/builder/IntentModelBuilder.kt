package com.promethist.core.builder

import com.promethist.core.Dialogue
import java.util.*

interface IntentModelBuilder {

    fun build(modelId: String, name: String, language: Locale, intents: List<Dialogue.Intent>)
}