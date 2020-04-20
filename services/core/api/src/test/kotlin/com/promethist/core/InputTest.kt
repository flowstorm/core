package com.promethist.core

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InputTest {

    @Test
    fun `input`() {
        val intentName = "-1"
        val input = Input(
                transcript = Input.Transcript("I see a dog, a red rose and a cat."),
                classes = mutableListOf(Input.Class(Input.Class.Type.Intent, intentName)),
                tokens = mutableListOf(
                        Input.Word("i"),
                        Input.Word("see"),
                        Input.Word("dog", mutableListOf(Input.Class(Input.Class.Type.Entity, "animal"))),
                        Input.Punctuation(","),
                        Input.Word("red", mutableListOf(Input.Class(Input.Class.Type.Entity, "B-flower"))),
                        Input.Word("rose", mutableListOf(Input.Class(Input.Class.Type.Entity, "I-flower"))),
                        Input.Word("and"),
                        Input.Word("cat", mutableListOf(Input.Class(Input.Class.Type.Entity, "animal"))),
                        Input.Punctuation(".")
                )
        )
        //println(input.words.entities("animal"))
        assertEquals(intentName, input.intent.name)
        assertEquals(2, input.entityMap.size)
        assertEquals(listOf("dog", "cat"), input.entities("animal"))
    }
}