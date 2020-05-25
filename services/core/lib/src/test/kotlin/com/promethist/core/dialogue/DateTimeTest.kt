package com.promethist.core.dialogue

import com.promethist.core.dialogue.BasicDialogue.Companion.isDay
import com.promethist.core.type.DateTime
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DateTimeTest : DialogueTest() {

    @Test
    fun `datetime`() {
        val dt = DateTime.parse("2020-05-25T11:42:56.355Z")

        val today = BasicDialogue.today
        val d1 = dt.day(-1)
        val d2 = dt.day(1)

        println(today)
        println(d1)
        println(d2)
        //println(d1 >= )
        println(dt isDay -1..0)
    }
}