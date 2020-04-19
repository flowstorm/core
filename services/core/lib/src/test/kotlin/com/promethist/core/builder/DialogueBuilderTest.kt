package com.promethist.core.builder

import com.promethist.core.builder.DialogueSourceCodeBuilder.*
import io.mockk.mockk
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.TestInstance
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DialogueBuilderTest {

    val dialogueBuilder = DialogueBuilder().apply {
        fileResource = mockk(relaxed = true)
        intentModelBuilder = mockk(relaxed = true)
    }

    @Test
    fun `test dialogue building`() {
        dialogueBuilder.create("product/dialogue/1").apply {
            source.apply {
                parameters = mapOf("str" to "bla", "num" to 123, "chk" to true)
                initCode = "val i = 1"

                var nodeId = 0
                addNode(Response(--nodeId, "response1", listOf("hello, say some animal", "hi, say some animal")))
                addNode(Intent(--nodeId, "intent1", listOf("no", "nope", "quit", "stop")))
                addNode(Intent(--nodeId, "intent2", listOf("dog", "cat", "tiger")))
                addNode(Function(--nodeId, "function1", mapOf("trans1" to "stop"), "println(trans1)\ntrans1"))
                addNode(Response(--nodeId, "response2", listOf("Your response was \${input.transcript.text}. Intent node \${input.intent.name}. Recognized entities: \${input.entitiesToString()}.")))
                addNode(UserInput(--nodeId, "input1", listOf("intent1", "intent2"), false, mapOf(), ""))

                addTransition("start" to "response1")
                addTransition("response1" to "input1")
                addTransition("intent1" to "function1")
                addTransition("intent2" to "response2")
                addTransition("response2" to "stop")
            }

            addResource(object : DialogueBuilder.Resource {
                override val stream: InputStream
                    get() = """{"data":"value"}""".byteInputStream()
                override val filename: String
                    get() = "data.json"

            })
        }
    }
}