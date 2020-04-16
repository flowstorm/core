package com.promethist.core.components

import com.promethist.core.Component
import com.promethist.core.Context
import com.promethist.core.dialogue.tokenize
import com.promethist.util.LoggerDelegate

class InternalTokenizer : Component {

    private val logger by LoggerDelegate()

    override fun process(context: Context): Context {

        with (context.input) {
            if (tokens.isEmpty()) {
                // tokenization has not been done yet (ASR skipped?)
                tokens.addAll(transcript.text.tokenize())
                context.logger.info("processing tokenization - tokens $tokens")
            } else {
                logger.info("processing tokenization - nothing to do")
            }
        }
        return context.pipeline.process(context)
    }
}