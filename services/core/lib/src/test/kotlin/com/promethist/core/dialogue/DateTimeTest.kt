package com.promethist.core.dialogue

import com.promethist.core.dialogue.BasicDialogue.Companion.isDay
import com.promethist.core.type.DateTime
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DateTimeTest : DialogueTest() {

    @Test
    fun `datetime`() {
        val t = BasicDialogue.now
        val d = BasicDialogue.today

        println(t)
        println(t + 1.second)
        println(t + 1.minute)
        println(t + 1.hour)

        println(d)
        println(d + 1.day)
        println(d + 1.month)
        println(d + 1.year)
        println(d - 1.day)
        println(d - 1.month)
        println(d - 1.year)
    }
}