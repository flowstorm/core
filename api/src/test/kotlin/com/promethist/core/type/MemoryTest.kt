package com.promethist.core.type

import com.promethist.common.ObjectUtil
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.properties.Delegates
import kotlin.properties.ObservableProperty
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.jvmName
import kotlin.test.assertEquals
import kotlin.test.assertSame

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MemoryTest {

    open class Parent(open val name: String) {
        override fun toString(): String {
            return "Parent(name='$name')"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as Parent
            return name == other.name
        }
    }

    data class Child(override val name: String, val age: Int): Parent(name) {
        override fun toString(): String {
            return "Child(name='$name', age=$age)"
        }
    }

    data class ComplicatedChild(override val name: String, val map: Map<String, List<Child>>,
                                val map2: Map<Int, String>, val parents: List<Parent>): Parent(name)

    val e1 = Parent("bla")
    val e2 = Child("blabla", 12)
    val e3 = ComplicatedChild("hm", mapOf("a" to listOf(Child("c1", 12), Child("c2", 13))),
            mapOf(1 to "b", 2 to "c"), listOf(Parent("papa")))

    @Test
    fun `test custom class in a list`() {
        val list = mutableListOf(e1, e2, e3)
        val memList = Memorable.pack(list)
        val s = ObjectUtil.defaultMapper.writeValueAsString(memList)
        val restored = ObjectUtil.defaultMapper.readValue(s, Memory::class.java)
        (restored._value as List<*>).forEach {
            assert(it is Map<*, *>)
        }
        val unpacked = Memorable.unpack(restored) as List<Parent>
        list.zip(unpacked).forEach { (orig, restored) ->
            assertEquals(orig, restored)
        }
    }

    @Test
    fun `test custom class in a map`() {
        val origMap = mutableMapOf("a" to e1, "b" to e2, "c" to e3)
        val memMap = Memorable.pack(origMap)
        val s = ObjectUtil.defaultMapper.writeValueAsString(memMap)
        val restored = ObjectUtil.defaultMapper.readValue(s, Memory::class.java)
        (restored._value as Map<*, *>).forEach {
            assert(it.value is Map<*, *>)
        }
        val unpacked = Memorable.unpack(restored) as Map<String, Parent>
        origMap.entries.zip(unpacked.entries).forEach { (orig, restored) ->
            assertEquals(orig, restored)
        }
    }

    @Test
    fun `test multiple classes`() {
        val list = mutableListOf(e1, e2, e3, 1, 1.5, 5L, 6.3F, "", true, false, StringMutableList("1", "2", "3"))
        list.forEach {
            val mem = Memorable.pack(it)
            val s = ObjectUtil.defaultMapper.writeValueAsString(mem)
            val restored = ObjectUtil.defaultMapper.readValue(s, Memory::class.java)
            assert(Memory.canContain(it) || restored._value is Map<*, *>)
            val unpacked = Memorable.unpack(restored)
            assertEquals(it, unpacked)
        }
    }

    @Test
    fun `test primitives in a collection`() {
        val list = mutableListOf(e1, e2, e3, 1, 1.5, 5L, 6.3F, "", true, false, StringMutableList("1", "2", "3"))
        val memList = Memorable.pack(list)
        val s = ObjectUtil.defaultMapper.writeValueAsString(memList)
        val restored = ObjectUtil.defaultMapper.readValue(s, Memory::class.java)
        (restored._value as List<*>).forEach {
            assert(Memory.canContain(it!!) || restored is Map<*, *>)
        }
        val unpacked = Memorable.unpack(restored) as List<Any>
        list.zip(unpacked).forEach { (orig, restored) ->
            assertEquals(orig, restored)
        }
    }

    @Test
    fun `test memorable collection`() {
        val list = MemoryMutableList(listOf(e1, e2, e3, "1", "2", "3").map { Memory(it) })
        val memList = Memorable.pack(list)
        assertSame(list, memList)
        val s = ObjectUtil.defaultMapper.writeValueAsString(memList)
        val restoredList = Memorable.unpack(ObjectUtil.defaultMapper.readValue(s, MemoryMutableList::class.java)) as MemoryMutableList<Any>
        list.zip(restoredList).forEach { (orig, restored) ->
            (restored)._value = Memorable.unpack(restored)
            assertEquals(orig, restored)
        }
    }
}