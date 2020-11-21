package com.promethist.core

import org.junit.jupiter.api.Assertions.assertEquals
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
        assertEquals(intentName, input.intent.name)
        assertEquals(2, input.entityMap.size)
        assertEquals(listOf("dog", "cat"), input.entities("animal").map { it.text })
    }

    @Test
    fun testMultiModelEntity() {
        val intentName = "-1"
        val input = Input(
                transcript = Input.Transcript("I saw president Barack Obama, Michelle Obama and their dog."),
                classes = mutableListOf(Input.Class(Input.Class.Type.Intent, intentName)),
                tokens = mutableListOf(
                        Input.Word("i"),
                        Input.Word("saw"),
                        Input.Word("president", mutableListOf(Input.Class(Input.Class.Type.Entity, "B-PER", model_id = "model2"))),
                        Input.Word("barack", mutableListOf(Input.Class(Input.Class.Type.Entity, "B-PER", model_id = "model1"),
                                                                 Input.Class(Input.Class.Type.Entity, "I-PER", model_id = "model2"))),
                        Input.Word("obama", mutableListOf(Input.Class(Input.Class.Type.Entity, "I-PER", model_id = "model1"),
                                                               Input.Class(Input.Class.Type.Entity, "I-PER", model_id = "model2"))),
                        Input.Punctuation(","),
                        Input.Word("michelle", mutableListOf(Input.Class(Input.Class.Type.Entity, "B-PER", model_id = "model1"))),
                        Input.Word("obama", mutableListOf(Input.Class(Input.Class.Type.Entity, "I-PER", model_id = "model1"))),
                        Input.Word("and"),
                        Input.Word("their"),
                        Input.Word("dog", mutableListOf(Input.Class(Input.Class.Type.Entity, "animal", model_id = "model2"))),
                        Input.Punctuation(".")
                )
        )
        assertEquals(intentName, input.intent.name)
        assertEquals(2, input.entityMap.size)
        assertEquals(listOf("dog"), input.entities("animal").map { it.text })
        assertEquals(listOf("president barack obama", "barack obama", "michelle obama"), input.entities("PER").map { it.text })
    }

    @Test
    fun testInvalidEntityAnnotation() {
        val intentName = "-1"
        val input = Input(
                transcript = Input.Transcript("I saw president Barack Obama and Michelle Obama."),
                classes = mutableListOf(Input.Class(Input.Class.Type.Intent, intentName)),
                tokens = mutableListOf(
                        Input.Word("i"),
                        Input.Word("saw"),
                        Input.Word("president"),
                        Input.Word("barack", mutableListOf(Input.Class(Input.Class.Type.Entity, "B-PER", model_id = "model1"),
                                                                 Input.Class(Input.Class.Type.Entity, "I-PER", model_id = "model2"))),
                        Input.Word("obama", mutableListOf(Input.Class(Input.Class.Type.Entity, "I-PER", model_id = "model1"),
                                                               Input.Class(Input.Class.Type.Entity, "I-PER", model_id = "model2"))),
                        Input.Word("and"),
                        Input.Word("michelle", mutableListOf(Input.Class(Input.Class.Type.Entity, "I-PER", model_id = "model1"))),
                        Input.Word("obama", mutableListOf(Input.Class(Input.Class.Type.Entity, "I-PER", model_id = "model1"))),
                        Input.Punctuation(".")
                )
        )
        assertEquals(intentName, input.intent.name)
        assertEquals(1, input.entityMap.size)
        assertEquals(listOf("barack obama", "barack obama", "michelle obama"), input.entities("PER").map { it.text })
    }

    @Test
    fun testInvalidEntityAnnotation2() {
        val intentName = "-1"
        val input = Input(
                transcript = Input.Transcript("I saw president Barack Obama."),
                classes = mutableListOf(Input.Class(Input.Class.Type.Intent, intentName)),
                tokens = mutableListOf(
                        Input.Word("i"),
                        Input.Word("saw"),
                        Input.Word("president"),
                        Input.Word("barack", mutableListOf(Input.Class(Input.Class.Type.Entity, "B-PER", model_id = "model1"))),
                        Input.Word("obama", mutableListOf(Input.Class(Input.Class.Type.Entity, "I-LOC", model_id = "model1"))),
                        Input.Punctuation(".")
                )
        )
        assertEquals(intentName, input.intent.name)
        assertEquals(2, input.entityMap.size)
        assertEquals(listOf("barack"), input.entities("PER").map { it.text })
        assertEquals(listOf("obama"), input.entities("LOC").map { it.text })
    }
}