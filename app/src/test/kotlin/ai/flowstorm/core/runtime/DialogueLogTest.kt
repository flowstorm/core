package ai.flowstorm.core.runtime

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import ai.flowstorm.core.model.LogEntry

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