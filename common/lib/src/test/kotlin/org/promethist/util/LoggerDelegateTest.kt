package org.promethist.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.slf4j.Logger

internal class LoggerDelegateTest {

    val loggerDelegate = LoggerDelegate()

    @Test
    fun `delegate provides same logger for class and its companion object`() {
        val classLogger = loggerDelegate.getValue(SampleClass, SampleClass::logger)
        val companionLogger = loggerDelegate.getValue(SampleClass, SampleClass.Companion::logger)
        assertEquals(classLogger, companionLogger)
    }

    class SampleClass {
        val logger: Logger? = null

        companion object {
            val logger: Logger? = null
        }
    }
}