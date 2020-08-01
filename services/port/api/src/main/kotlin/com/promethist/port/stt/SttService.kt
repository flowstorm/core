package com.promethist.port.stt

import com.promethist.core.ExpectedPhrase

interface SttService: AutoCloseable {

    fun createStream(config: SttConfig, expectedPhrases: List<ExpectedPhrase>): SttStream

}