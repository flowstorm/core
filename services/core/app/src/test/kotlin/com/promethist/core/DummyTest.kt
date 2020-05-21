package com.promethist.core

import com.fasterxml.jackson.module.kotlin.readValue
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

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DummyTest {
    //Dummy test to keep test directory in git and avoid warnings in build until we introduce actual tests.

    data class TestX(val id: String, val attributes: Attributes = Attributes())

    @Test
    fun `dummy test case`() {

        /*
        val db = KMongo.createClient(ConnectionString(AppConfig.instance["database.url"]))
                .getDatabase(AppConfig.instance["name"] + "-" + AppConfig.instance["namespace"])

        println(AppConfig.instance["name"] + "-" + AppConfig.instance["namespace"])
*/

        val t1 = TestX("xxx")
        t1.attributes.get("ns1").apply {
            put("iv", Value(1).apply { value++ })
            put("bv", Value(true))
            put("dtv", Value(ZonedDateTime.now()))
            put("dtml", Value(DateTimeMutableList(ZonedDateTime.now())))

            put("sl", Value(StringMutableList("a", "b", "c")))
            put("is", Value(IntMutableSet(1, 2, 3)))

            //put("bvl", Value(BooleanValueMutableList(Value(true), Value(false))))
            //put("svl", Value(StringValueMutableList(Value("a"), Value("b"))))
            //put("ivl", Value(IntValueMutableList(Value(1), Value(2))))
            put("lvl", Value(LongValueMutableList(Value(1), Value(2))))
            put("fvl", Value(FloatValueMutableList(Value(.0F), Value(.0F))))
            //put("dvl", Value(DoubleValueMutableList(Value(.0), Value(.0))))
            put("bdvl", Value(BigDecimalValueMutableList(Value(BigDecimal.valueOf(1)), Value(BigDecimal.valueOf(2)))))

            //put("bvs", Value(BooleanValueMutableSet(Value(true), Value(false))))
            //put("svs", Value(StringValueMutableSet(Value("a"), Value("b"))))
            //put("ivs", Value(IntValueMutableSet(Value(1), Value(2))))
            put("lvs", Value(LongValueMutableSet(Value(1), Value(2))))
            put("fvs", Value(FloatValueMutableSet(Value(.0F), Value(.0F))))
            //put("dvs", Value(DoubleValueMutableSet(Value(.0), Value(.0))))
            put("bdvs", Value(BigDecimalValueMutableSet(Value(BigDecimal.valueOf(1)), Value(BigDecimal.valueOf(2)))))
        }

        val json = mapper.writeValueAsString(t1)
        println(json)
        //mapper.writerWithDefaultPrettyPrinter().writeValue(System.out, t1)
        val t2 = mapper.readValue<TestX>(json)

        println(t1 == t2)

/*
        val col = db.getCollection<TestX>("testx")
        col.insertOne(t1)

        val t2 = col.findOne { TestX::id eq "xxx" }
        println(t2)
*/
        val iv1 = t1!!.attributes["ns1"]["iv"]
        val iv2 = t2!!.attributes["ns1"]["iv"]

        println(1)
    }
}