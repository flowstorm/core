package com.promethist.core.runtime

import com.promethist.core.type.Dynamic
import com.promethist.core.type.PropertyMap
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ApiTest {

    @Test
    fun `test api words`() {

        val antonyms = Api.words<ArrayList<String>>("good", "antonyms")
        println(antonyms)

        val response = Api.words<Dynamic>("hatchback") as Dynamic
        response<ArrayList<PropertyMap>>("results") {
            value.forEach {
                println("partOfSpeech = " + it["partOfSpeech"])
                if (it.containsKey("synonyms")) {
                    println("synonyms:")
                    (it["synonyms"] as ArrayList<String>).forEach {
                        println("- $it")
                    }
                }
            }
        }
    }
}