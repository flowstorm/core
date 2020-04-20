package com.promethist.core.type

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
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
    fun `modify value using lambda`() {
        val mem = Dynamic()
        mem("counter", 1)
        mem<Int>("counter") { value = 10; Unit }
        assertEquals(10, mem("counter"))
    }

    @Test
    fun `return value comes from lambda`() {
        val mem = Dynamic()
        val result = mem<Int>("counter") { value = 10; "result" }
        assertEquals("result", result)
    }

    @Test
    fun `access nested values using lambda`() {
        val mem = Dynamic()
        mem("counter.nested", 10)
        mem<Dynamic>("counter") {
            value<Int>("nested") {
                assertEquals(10, value)
            }
        }
    }

    @Test
    fun `test list`() {
        val mem = Dynamic()
        mem.list<Int>("list") { value.add(1) }
        mem.list<Int>("list") { value.add(2) }

        assertEquals(2, mem.list<Int>("list").size)
        assertTrue(mem.list<Int>("list").contains(1))
        assertTrue(mem.list<Int>("list").contains(2))
    }

    @Test
    fun `test set`() {
        val mem = Dynamic()
        mem.set<Int>("list") { value.add(1) }
        mem.set<Int>("list") { value.add(1) } //add same again

        assertEquals(1, mem.set<Int>("list").size)
        assertTrue(mem.set<Int>("list").contains(1))
    }
}