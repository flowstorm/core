package ai.flowstorm.core.runtime

import ai.flowstorm.core.model.LogEntry
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ContextLogTest {

    @Test
    fun `test logger`() {
        val contextLog = ContextLog()

        contextLog.logger.info("Log something")

        with(contextLog.log.first()) {
            assertEquals(LogEntry.Level.INFO, level)
            assertEquals("Log something", text)
        }
    }
}