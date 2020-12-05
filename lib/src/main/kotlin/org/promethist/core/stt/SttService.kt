package org.promethist.core.stt

import org.promethist.core.ExpectedPhrase
import org.promethist.core.model.SttConfig

interface SttService: AutoCloseable {

    fun createStream(config: SttConfig, expectedPhrases: List<ExpectedPhrase>): SttStream

}