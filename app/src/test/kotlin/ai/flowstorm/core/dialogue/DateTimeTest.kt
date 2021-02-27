package ai.flowstorm.core.dialogue

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DateTimeTest : DialogueTest() {

    @Test
    fun `datetime`() {
        with (dialogue) {

            val t = now
            val d = today

            println("now = $t")
            println("today = $d")
            println("yesterday = $yesterday")
            println("tomorrow = $tomorrow")
            println((now + (if (now.hour in 18..23) 1 else 0).day).set(hour = 19, minute = 0, second = 0))

            val d_5 = today + 5.day
            println("d-5 = $d_5")

            println(d_5 isDay -4..4)

        }

    }
}