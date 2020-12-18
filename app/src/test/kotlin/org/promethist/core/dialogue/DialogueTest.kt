package org.promethist.core.dialogue

import io.mockk.every
import io.mockk.mockkClass
import io.mockk.mockkObject
import org.promethist.core.Context
import org.promethist.core.dialogue.metric.MetricDelegate
import org.promethist.core.model.metrics.Metric
import org.promethist.core.type.Attributes
import org.promethist.core.type.DEFAULT_LOCATION
import org.slf4j.LoggerFactory
import java.time.ZoneId

open class DialogueTest {

    class TestDialogue : BasicDialogue() {
        override val dialogueId = "dialogue1"
        override val dialogueName = "product/dialogue"
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