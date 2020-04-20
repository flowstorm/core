package com.promethist.core.type

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DynamicTest {

    @Test
    fun `assign value`() {
        val mem = Dynamic()
        mem("counter", 10)
        assertEquals(10, mem("counter"))
    }

    @Test
    fun `assign nested value`() {
        val mem = Dynamic()
        mem("counter.nested", 10)
        assertEquals(10, mem("counter.nested"))
    }

    @Test
    fun `assign value using lambda`() {
        val mem = Dynamic()
        mem<Int>("counter") { value = 10; Unit }
        assertEquals(10, mem("counter"))
    }

    @Test
    fun `assign nested value using lambda`() {
        val mem = Dynamic()
        mem<Int>("counter.nested") { value = 10; Unit }
        assertEquals(10, mem("counter.nested"))
    }

    @Test
    fun `return value comes from lambda`() {
        val mem = Dynamic()
        val result = mem<Int>("counter") { value = 10; "result" }
        assertEquals("result", result)
    }
}