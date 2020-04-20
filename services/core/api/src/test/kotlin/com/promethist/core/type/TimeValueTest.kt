package com.promethist.core.type

import com.fasterxml.jackson.module.kotlin.readValue
import com.promethist.common.ObjectUtil.defaultMapper as mapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TimeValueTest {

    @Test
    fun `serialize value`() {
        val v1 = TimeInt(0)
        val json = mapper.writeValueAsString(v1)
        val v2 = mapper.readValue<TimeInt>(json)
        assertEquals(v1, v2)
    }
}
