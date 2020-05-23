package com.promethist.core

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.readValue
import com.mongodb.ConnectionString
import com.promethist.common.ObjectUtil.defaultMapper as mapper
import com.promethist.common.AppConfig
import com.promethist.core.type.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.litote.kmongo.*
import org.litote.kmongo.id.jackson.IdJacksonModule
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.time.ZonedDateTime

typealias ValueMutableList2 = MutableList<Value<*>>

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DummyTest {
    //Dummy test to keep test directory in git and avoid warnings in build until we introduce actual tests.

    data class TestX(val id: String, val attributes: Attributes = Attributes())

    data class TestY(val id: String, val v: PersistentObject, val ivl: ValueMutableList2, val svs: ValueMutableList2, val dtvl: ValueMutableList)

    val db get() = KMongo.createClient(ConnectionString(AppConfig.instance["database.url"]))
            .getDatabase(AppConfig.instance["name"] + "-" + AppConfig.instance["namespace"])

    val tx1 = TestX("id").apply {
        attributes.get("ns1").apply {
            put("iv", Value(1).apply { value++ })
            put("bv", Value(true))
            put("dtv", Value(ZonedDateTime.now()))

            put("dtml", Value(DateTimeMutableList(ZonedDateTime.now())))
            put("sl", Value(StringMutableList("a", "b", "c")))
            put("il", Value(IntMutableList(1, 2, 3)))
            put("ss", Value(StringMutableSet("d", "e", "f")))
            put("is", Value(IntMutableSet(4, 5, 6)))

            put("bvl", ValueMutableList(Value(true), Value(false)))
            put("svl", ValueMutableList(Value("a"), Value("b")))
            put("ivl", ValueMutableList(Value(1), Value(2)))
            put("lvl", ValueMutableList(Value(1), Value(2)))
            put("fvl", ValueMutableList(Value(.0F), Value(.0F)))
            put("dvl", ValueMutableList(Value(.0), Value(.0)))
            put("bdvl", ValueMutableList(Value(BigDecimal.valueOf(1)), Value(BigDecimal.valueOf(2))))

            put("bvs", ValueMutableSet(Value(true), Value(false)))
            put("svs", ValueMutableSet(Value("c"), Value("d")))
            put("ivs", ValueMutableSet(Value(3), Value(4)))
            put("lvs", ValueMutableSet(Value(3), Value(4)))
            put("fvs", ValueMutableSet(Value(.1F), Value(.1F)))
            put("dvs", ValueMutableSet(Value(.1), Value(.1)))
            put("bdvs", ValueMutableSet(Value(BigDecimal.valueOf(3)), Value(BigDecimal.valueOf(4))))
        }
    }

    val ty1 = TestY("id", ValueMutableList(Value(3), Value(4)),
            mutableListOf(Value(1), Value(2)),
            mutableListOf(Value("a"), Value("b")),
            ValueMutableList(Value(DateTime.now())))

    init {
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true)
    }

    @Test
    fun `dummy test x db`() {

        val json = mapper.writeValueAsString(tx1)
        println(json)

        val col = db.getCollection<TestX>("testx")
        col.insertOne(tx1)

        val tx2 = col.findOne { TestX::id eq "id" }
        println(tx2)
    }

    @Test
    fun `dummy y json`() {
        val json = mapper.writeValueAsString(ty1)
        println(json)

        val ty2 = mapper.readValue<TestY>(json)
        println(ty2)

    }

    @Test
    fun `dummy y db`() {
        val col = db.getCollection<TestY>("testy")
        col.insertOne(ty1)

        val ty2 = col.findOne { TestX::id eq "id" }

        println(ty2)

    }
}
