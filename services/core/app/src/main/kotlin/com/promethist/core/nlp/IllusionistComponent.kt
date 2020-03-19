package com.promethist.core.nlp

import org.slf4j.LoggerFactory

class IllusionistComponent : Component {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun process(context: Context): Context {
        logger.info("Illusionist NLP adapter called")

        return context
    }
}