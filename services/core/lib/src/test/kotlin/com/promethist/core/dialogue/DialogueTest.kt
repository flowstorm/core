package com.promethist.core.dialogue

import com.promethist.core.Context
import com.promethist.core.model.metrics.Metric
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.mockkObject
import java.time.ZoneId

open class DialogueTest {

    class TestDialogue : BasicDialogue() {
        override val dialogueName = "product/dialogue/1"
        var metric by MetricDelegate("namespace.name")
    }

    val metrics = mutableListOf<Metric>()
    val dialogue = TestDialogue()
    val context = mockkClass(Context::class)

    init {
        every { context.session.metrics } returns metrics
        mockkObject(Dialogue)
        every { Dialogue.run } returns Dialogue.Run(dialogue, context)
        every { context.turn.input.zoneId } returns ZoneId.of("Europe/Paris")
    }
}