package com.promethist.core.runtime

import com.promethist.core.dialogue.DialogueTest
import com.promethist.core.type.Dynamic
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.Assertions.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ParkopediaApiTest : DialogueTest() {


    @Test
    fun test1() {
        with (dialogue) {
            println(parkopedia.nearParking())
        }
    }

    @Test
    fun testAddingData() {
        with (dialogue) {
            val record1 = Dynamic(
                    "name" to "Some Parking",
                    "price" to 5
            )
            parkopedia.addMockedData(record1)
            assertEquals(parkopedia.nearParking()[1], record1)
        }
    }
}