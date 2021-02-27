package ai.flowstorm.core.type

import org.junit.jupiter.api.TestInstance

typealias ValueMutableList2 = MutableList<Memory<*>>

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DummyTest {/*//temporatily commented - needs to FIX some dependency issue
    //Dummy test to keep test directory in git and avoid warnings in build until we introduce actual tests.

    data class TestX(val id: String, val attributes: Attributes = Attributes())

    val db get() = KMongo.createClient(ConnectionString(AppConfig.instance["database.url"]))
            .getDatabase(AppConfig.instance["name"] + "-" + AppConfig.instance["namespace"])

    val tx1 = TestX("id").apply {
        attributes.get("ns1").apply {
            put("iv", Memory(1).apply { value++ })
            put("bv", Memory(true))
            put("dtv", Memory(ZonedDateTime.now()))

            put("dtml", Memory(DateTimeMutableList(ZonedDateTime.now())))
            put("sl", Memory(StringMutableList("a", "b", "c")))
            put("il", Memory(IntMutableList(1, 2, 3)))
            put("ss", Memory(StringMutableSet("d", "e", "f")))
            put("is", Memory(IntMutableSet(4, 5, 6)))

            put("bvl", MemoryMutableList(Memory(true), Memory(false)))
            put("svl", MemoryMutableList(Memory("a"), Memory("b")))
            put("ivl", MemoryMutableList(Memory(1), Memory(2)))
            put("lvl", MemoryMutableList(Memory(1), Memory(2)))
            put("fvl", MemoryMutableList(Memory(.0F), Memory(.0F)))
            put("dvl", MemoryMutableList(Memory(.0), Memory(.0)))
            put("bdvl", MemoryMutableList(Memory(Decimal.valueOf(1)), Memory(Decimal.valueOf(2))))

            put("bvs", MemoryMutableSet(Memory(true), Memory(false)))
            put("svs", MemoryMutableSet(Memory("c"), Memory("d")))
            put("ivs", MemoryMutableSet(Memory(3), Memory(4)))
            put("lvs", MemoryMutableSet(Memory(3), Memory(4)))
            put("fvs", MemoryMutableSet(Memory(.1F), Memory(.1F)))
            put("dvs", MemoryMutableSet(Memory(.1), Memory(.1)))
            put("bdvs", MemoryMutableSet(Memory(Decimal.valueOf(3)), Memory(Decimal.valueOf(4))))
        }
    }

    init {
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true)
    }

    @Test
    fun `dummy test x db`() {
        /*
        val json = mapper.writeValueAsString(tx1)
        println(json)

        val col = db.getCollection<TestX>("testx")
        col.insertOne(tx1)

        val tx2 = col.findOne { TestX::id eq "id" }
        println(tx2)
        */
    }*/
}
