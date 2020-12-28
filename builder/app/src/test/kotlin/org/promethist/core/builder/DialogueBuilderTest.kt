package org.promethist.core.builder

import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DialogueBuilderTest {

    private val dialogueBuilder = DialogueBuilder().apply {
        fileResource = mockk(relaxed = true)
        intentModelBuilder = mockk(relaxed = true)
    }

    @Test
    fun `test dialogue building`() {
        /*
        dialogueBuilder.create(sourceCodeBuilder.build()).apply {

            addResource(object : DialogueBuilder.Resource {
                override val stream: InputStream
                    get() = """{"data":"value"}""".byteInputStream()
                override val filename: String
                    get() = "data.json"

            })

            build()
        }
        */
    }
}