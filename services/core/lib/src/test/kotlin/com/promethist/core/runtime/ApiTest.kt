package com.promethist.core.runtime

import com.promethist.core.type.Dynamic
import com.promethist.core.type.PropertyMap
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ApiTest {

    @Test
    fun `test api words`() {
        /*
        val api = Api()

        println(api.get<List<String>>("https://repository.promethist.ai/data/animals.json"))

        val antonyms = api.words<ArrayList<String>>("good", "antonyms")
        println(antonyms)

        val response = api.words<Dynamic>("hatchback")
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
        */
    }
}