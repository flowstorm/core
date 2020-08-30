package com.promethist.core.runtime

import com.promethist.core.dialogue.DialogueTest
import com.promethist.core.type.Dynamic
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.Assertions.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ParkopediaApiTest : DialogueTest() {

    @Test
    fun testAddingData() {
        with (dialogue) {
            val record1 = Dynamic(
                    "name" to "Some Parking",
                    "price" to 5
            )
            parkopedia.addMockedData(record1)
            assertEquals(parkopedia.nearParking()[200], record1)
        }
    }
}