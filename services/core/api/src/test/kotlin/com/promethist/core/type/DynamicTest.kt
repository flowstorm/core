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

    @Test @Disabled("Should work, bug in Dynamic?")
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

    @Test @Disabled("Should work, bug in Dynamic?")
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

    @Test
    fun `dynamic test case`() {
        val mem = Dynamic()
        mem("counter.a", 1) // direct assignment (no need to declare type - taken from input value)
        mem<Int>("counter.a") { ++value } // lambda assignment - note: must always return any value:
        mem<Int>("counter.a") {
            value += 1
            Unit
        }
        mem<Int>("counter.b") { ++value }
        mem<Dynamic>("counter") {
            value<Int>("a") {
                println("a=$value")
            }
            value<Int>("b") {
                println("b=$value")
            }
        }

        if (mem<Int>("level") { apply { value = if (value == 0) 1 else 2 }.value } == 1)

        mem("user.name", "John") // another direct assignment
        val node = mem<String>("user.name") {
            value = "Jerry"
            -5
        }
        println(node)

        mem.set<Int>("col.set") { value.add(1) }
        mem.set<Int>("col.set") { value.add(2) }
        mem.list<Int>("col.list") { value.add(3) }
        mem.list<Int>("col.list") { value.add(4) }

        println(mem("col"))
        println(mem("counter.a") == 2)
        println(mem("user.name") == "Jerry")
        println(mem.set<Int>("col.set").contains(1))

        // list of time memories
        mem.set<TimeString>("animals") {
            value.add(TimeString("dog"))
            value.add(TimeString("cat"))
            value.add(TimeString("dog"))
        }
        mem.set<TimeString>("animals").find { it.value == "cat" }?.let {
            println("cat remembered ${it.time}")
        }
    }
}