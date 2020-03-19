package com.promethist.core.nlp.intentRecognition

import com.promethist.core.Context
import com.promethist.core.nlp.NlpAdapter
import org.slf4j.LoggerFactory

class IllusionistNlpAdapter : NlpAdapter {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun process(context: Context): Context {
        logger.info("Illusionist NLP adapter called")

        return context
    }
}