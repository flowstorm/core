package org.promethist.core.runtime

import org.promethist.core.dialogue.DialogueTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SampleApiTest : DialogueTest() {


    @Test
    fun `test1`() {
        with (dialogue) {
            println(sample.test1())
        }
    }
}