package com.promethist.core.components

import com.promethist.core.Component
import com.promethist.core.Context
import com.promethist.util.LoggerDelegate
import org.slf4j.LoggerFactory

class InternalTokenizer : Component {

    private val logger by LoggerDelegate()

    override fun process(context: Context): Context {

        logger.info("processing tokenization")

        //TODO check/do context.input tokenization

        return context
    }
}