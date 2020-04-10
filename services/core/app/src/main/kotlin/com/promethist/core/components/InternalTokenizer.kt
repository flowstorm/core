package com.promethist.core.components

import com.promethist.core.Component
import com.promethist.core.Context
import com.promethist.core.Input
import com.promethist.util.LoggerDelegate
import java.util.*

class InternalTokenizer : Component {

    private val logger by LoggerDelegate()

    override fun process(context: Context): Context {

        with (context.input) {
            if (tokens.isEmpty()) {
                // tokenization has not been done yet (ASR skipped?)
                logger.info("processing tokenization")
                val tokenizer = StringTokenizer(transcript.text, " \t\n\r,.:;?![]'")
                while (tokenizer.hasMoreTokens()) {
                    tokens.add(Input.Word(tokenizer.nextToken().toLowerCase()))
                }
                context.logger.info("input tokens $tokens")
            } else {
                logger.info("processing tokenization - nothing to do")
            }
        }
        return context.pipeline.process(context)
    }
}