package com.promethist.core.dialogue

import com.promethist.core.model.metrics.Metric
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MetricDelegateTest : DialogueTest() {

    @BeforeEach
    fun clearMetrics() = metrics.clear()

    @Test
    fun `delegate updates metric in session`() {
        dialogue.metric = 5
        dialogue.metric = 10
        assertEquals(1, metrics.size)
        assertEquals(10L, metrics.first().value)
    }

    @Test
    fun `increment metric`() {
        metrics.add(Metric("namespace", "name", 10L))
        dialogue.metric++
        assertEquals(11L, metrics.first().value)
    }
}