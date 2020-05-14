package com.promethist.core.dialogue

import com.promethist.core.Context
import com.promethist.core.model.metrics.Metric
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.mockkObject
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MetricDelegateTest {

    class SampleDialogue : Dialogue() {
        override val dialogueName = "product/dialog/1"
        var metric by MetricDelegate("namespace.name")
    }

    private val metrics = mutableListOf<Metric>()
    private val dialogue = SampleDialogue()
    private val context = mockkClass(Context::class)

    init {
        every { context.session.metrics } returns metrics
        mockkObject(Dialogue.Companion)
        every { Dialogue.threadContext() } returns Dialogue.ThreadContext(dialogue, context)
    }

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