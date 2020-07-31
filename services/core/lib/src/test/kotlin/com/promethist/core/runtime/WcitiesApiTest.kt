package com.promethist.core.runtime

import com.promethist.core.dialogue.DialogueTest
import com.promethist.core.type.Dynamic
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class WcitiesApiTest : DialogueTest() {


    @Test
    fun test1() {
        with (dialogue) {
            println(wcities.nearCity())
            println(wcities.events())
            println(wcities.records(category = 1))
            println(wcities.movies())
            println(wcities.theaters())
        }
    }

    @Test
    fun testAddingData() {
        with (dialogue) {
            val record1 = Dynamic(
                    "name" to "Warwick Castle",
                    "id" to "123",
                    "category" to "castle",
                    "address" to "Address.",
                    "short_desc" to "It is a medieval castle developed from a wooden fort, originally built by William the Conqueror during 1068.",
                    "desc" to "The original wooden motte-and-bailey castle was rebuilt in stone during the 12th century. During the Hundred Years War, the facade opposite the town was refortified, resulting in one of the most recognisable examples of 14th-century military architecture. It was used as a stronghold until the early 17th century, when it was granted to Sir Fulke Greville by James I in 1604."
            )
            wcities.addMockedData(WcitiesApi.Type.RECORD, record1)
            assertEquals(wcities.withCustom(WcitiesApi.Type.RECORD) { wcities.records(category = 1) }.last(), record1)
        }
    }

}