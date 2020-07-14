package com.promethist.core.runtime

import com.promethist.core.dialogue.DialogueTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class WcitiesApiTest : DialogueTest() {


    @Test
    fun test1() {
        with (dialogue) {
            println(wcities.nearCities())
            println(wcities.events())
            println(wcities.poi(category = 1))
            println(wcities.movies())
            println(wcities.theaters())
        }
    }

}