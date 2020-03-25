package com.promethist.core.builder

import com.promethist.core.resources.FileResource
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.TestInstance
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DialogueModelBuilderTest {

    val fileResource:FileResource = mockk(relaxed = true)
    val intentModelBuilder:IntentModelBuilder =mockk(relaxed = true)

    @Test
    fun `test dialogue building`() {
        var nodeId = 0
        val builder = DialogueModelBuilder("product/dialogue/1", Locale("en"), mapOf(
                "str" to "bla",
                "num" to 123,
                "chk" to true
        ),"val i = 1").apply {
            addResponse(--nodeId, "response1", listOf("hello, say some animal", "hi, say some animal"))
            addIntent(--nodeId, "intent1", listOf("no", "nope", "quit", "stop"))
            addIntent(--nodeId, "intent2", listOf("dog", "cat", "tiger"))
            addFunction(--nodeId, "function1", mapOf("trans1" to "stop"), "println(trans1)\ntrans1")
            addResponse(--nodeId, "response2", listOf("Your response was \${input.text}. Intent node \${input.intent.name}. Recognized entities: \${input.entitiesToString()}."))
            //addSubDialogue(--nodeId, "subDialogue1", "product/subdialogue/1")

            // user inputs always at the end (all intents must be defined before)
            addUserInput(--nodeId, "input1", listOf("intent1", "intent2"), false)
            finalize(mapOf(
                    "start" to "response1",
                    "response1" to "input1",
                    "intent1" to "function1",
                    "intent2" to "response2",
                    "response2" to "stop"
                    //"subDialogue1" to "response1",
            ))
        }

        builder.build(intentModelBuilder, fileResource)

    }
}