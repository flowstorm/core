package com.promethist.port.stt

import com.promethist.core.ExpectedPhrase
import com.promethist.core.model.SttConfig

interface SttService: AutoCloseable {

    fun createStream(config: SttConfig, expectedPhrases: List<ExpectedPhrase>): SttStream

}