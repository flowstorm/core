package ai.flowstorm.core.type

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import ai.flowstorm.common.ObjectUtil

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

    interface X : Dynamic.Object, Y {
        val y: Y
    }

    interface Y {
        var a: Int
        var b: Int
    }

    val Y.aAsString get() = "a:$a"

    @Test
    fun `proxy`() {
        //val mem = Dynamic("b" to 2)
        //val obj = mem<X>()
        val obj = dynamic<X> {
            b = 2
        }
        obj.a = 1
        obj.y.b = 1
        println(obj.dynamic)
        println(obj.aAsString)
        assertEquals(1, obj.a)
        assertEquals(2, obj.b)
        assertEquals(0, obj.y.a)
        assertEquals(1, obj.y.b)

        // test equal
        val mem2 = Dynamic("a" to 1, "b" to 2, "y" to Dynamic("a" to 0, "b" to 1))
        assertEquals(obj.dynamic, mem2)

        // test json serialization
        println(ObjectUtil.defaultMapper.writeValueAsString(obj))
    }
}