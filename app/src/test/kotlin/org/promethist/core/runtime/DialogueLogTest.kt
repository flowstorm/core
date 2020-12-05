package org.promethist.core.runtime

import org.promethist.core.model.LogEntry
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DialogueLogTest {

    @Test
    fun `test logger`() {
        val log = DialogueLog()

        log.logger.info("Log something")

        with(log.log.first()) {
            assertEquals(LogEntry.Level.INFO, level)
            assertEquals("Log something", text)
        }
    }
}