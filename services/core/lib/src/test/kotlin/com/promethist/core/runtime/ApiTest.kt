package com.promethist.core.runtime

import com.promethist.core.type.Dynamic
import com.promethist.core.type.PropertyMap
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ApiTest {

    @Test
    fun `test api words`() {

        val api = Api()

        println(api.get<List<String>>(api.target("https://repository.promethist.ai/data/animals.json")))

        println(api.words("good", "antonyms"))

        val response = api.words<Dynamic>("hatchback")
        response<List<PropertyMap>>("results") {
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