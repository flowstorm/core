package com.promethist.core

import com.mongodb.ConnectionString
import com.promethist.common.AppConfig
import com.promethist.core.type.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.litote.kmongo.*
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


        val t1 = TestX("xxx")
        t1.attributes.get("ns1").apply {
            put("iv", IntValue(1))
            put("bv", BooleanValue(true))
            put("dtv", DateTimeValue(ZonedDateTime.now()))
            val dtml = DateTimeMutableList()
            dtml.add(ZonedDateTime.now())
            put("dtml", Value.pack(dtml))
        }

        println(t1)

        val col = db.getCollection<TestX>("testx")
        //col.insertOne(t1)

        val t2 = col.findOne { TestX::id eq "xxx" }
        println(t2)

        val iv1 = t1!!.attributes["ns1"]["iv"]
        val iv2 = t2!!.attributes["ns1"]["iv"]

        println(1)

         */
    }
}