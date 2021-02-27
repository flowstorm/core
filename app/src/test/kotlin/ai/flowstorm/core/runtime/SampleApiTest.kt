package ai.flowstorm.core.runtime

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import ai.flowstorm.core.dialogue.DialogueTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SampleApiTest : DialogueTest() {


    @Test
    fun `test1`() {
        with (dialogue) {
            println(sample.test1())
        }
    }
}