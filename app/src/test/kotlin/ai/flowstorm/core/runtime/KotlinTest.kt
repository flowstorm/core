package ai.flowstorm.core.runtime

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

internal class KotlinTest {

    @Test
    fun `create object without optional argument`() {
        val instance = Kotlin.newObjectWithArgs(TestClass1::class, mapOf("a" to 1))
        assertEquals(1, instance.a)
    }

    @Test
    fun `create object without required argument`() {
        assertThrows(RuntimeException::class.java) {
            Kotlin.newObjectWithArgs(TestClass1::class, mapOf())
        }
    }

    @Test
    fun `class without primary constructor`() {
        assertThrows(RuntimeException::class.java) {
            Kotlin.newObjectWithArgs(TestClass2::class, mapOf())
        }
    }

    @Test
    fun `construct without enough arguments`() {
        assertThrows(RuntimeException::class.java) {
            Kotlin.newObject(TestClass1::class)
        }
    }

    class TestClass1(val a: Int, val b: Int = 2)
    class TestClass2 {
        constructor(@Suppress("UNUSED_PARAMETER") a: Int)
    }
}