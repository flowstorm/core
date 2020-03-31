package com.promethist.core.model.metrics

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.TestInstance
import java.lang.IllegalArgumentException

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MetricsTest {

    val metrics = Metrics(listOf())

    @Test
    fun `non existing metric has default 0`() {
        assertEquals(0, metrics("test.metric1"))
    }

    @Test
    fun `setting metric value`() {
        metrics("test.metric2") { value = 10 }
        assertEquals(10, metrics("test.metric2"))
    }

    @Test
    fun `throws on invalid metric name`() {
        assertThrows(IllegalArgumentException::class.java) {
            metrics("invalid")
        }

        assertThrows(IllegalArgumentException::class.java) {
            metrics("invalid..invalid")
        }
    }
}