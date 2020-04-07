package com.promethist.core

import com.promethist.core.type.Dynamic
import com.promethist.core.type.TimeString
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DynamicTest {
    //Dummy test to keep test directory in git and avoid warnings in build until we introduce actual tests.

    @Test
    fun `dynamic test case`() {
        val mem = Dynamic()
        mem("counter.a", 1) // direct assignment (no need to declare type - taken from input value)
        mem<Int>("counter.a") { ++value } // lambda assignment - note: must always return any value:
        mem<Int>("counter.a") {
            value += 1
            Unit
        }

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
        println("dog remembered ${mem.set<TimeString>("animals").filter { it.value == "dog" }.size}x")
        println("all remembered animals ${mem.set<TimeString>("animals").filter { it.yesterday() }.map { it.value }}.")
    }
}