package com.promethist.core.dialogue

import com.promethist.core.Context
import com.promethist.core.dialogue.metric.MetricDelegate
import com.promethist.core.model.metrics.Metric
import com.promethist.core.type.Attributes
import com.promethist.core.type.DEFAULT_LOCATION
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.mockkObject
import org.slf4j.LoggerFactory
import java.time.ZoneId

open class DialogueTest {

    class TestDialogue : BasicDialogue() {
        override val dialogueId = "dialogue1"
        override val dialogueName = "product/dialogue/1"
        override var clientLocation = DEFAULT_LOCATION
        var metric by MetricDelegate("namespace.name")

        val response1 = Response({ "Hello" })
    }

    val metrics = mutableListOf<Metric>()
    val dialogue = TestDialogue()
    val context = mockkClass(Context::class)
    var attributes = Attributes()

    init {
        every { context.session.metrics } returns metrics
        every { context.session.attributes } returns attributes
        every { context.logger } returns LoggerFactory.getLogger(this.javaClass)
        mockkObject(AbstractDialogue)
        every { AbstractDialogue.run } returns AbstractDialogue.Run(dialogue.response1, context)
        every { context.turn.input.zoneId } returns ZoneId.of("Europe/Paris")
    }
}